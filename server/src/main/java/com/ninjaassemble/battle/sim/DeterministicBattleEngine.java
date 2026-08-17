package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * Authoritative deterministic continuous-time auto-combat simulation.
 *
 * <p>There is intentionally no global turn/round loop. Every actor owns an independent action timeline and the
 * simulator advances directly to the next scheduled logical event. "Simultaneous" combat is represented by one
 * stable priority queue, not by Java threads.</p>
 */
public final class DeterministicBattleEngine {
    private static final int MAX_RAGE = 100;
    private static final Set<String> DOT_STATUSES = Set.of("BURN", "POISON", "BLEED");
    private static final Set<String> NEGATIVE_STATUSES = Set.of("STUN", "SILENCE", "BURN", "POISON", "BLEED", "ATK_DOWN", "DEF_DOWN", "SPEED_DOWN");
    private static final Set<String> POSITIVE_STATUSES = Set.of("ATK_UP", "DEF_UP", "SPEED_UP");

    public BattleResult simulate(BattleRequest request) {
        if (request == null || request.ruleset() == null || request.units() == null || request.units().isEmpty()) {
            throw new IllegalArgumentException("battle request/ruleset/units required");
        }
        Context ctx = new Context(request.seed(), request.ruleset(), request.units());
        ctx.emit(0, BattleEventType.BATTLE_START, null, null, 0, false, null, null, null, 0, null, null, 0, null);

        for (UnitState unit : ctx.units.values()) {
            ctx.triggerPassives(PassiveTrigger.BATTLE_START, unit, null, 0, false);
        }
        for (UnitState unit : ctx.units.values()) {
            for (BattlePassive passive : unit.seed.passives()) {
                if (passive.trigger() == PassiveTrigger.TIME_INTERVAL) {
                    ctx.schedule(new ScheduledEvent(passive.intervalMs(), ScheduledType.PASSIVE_INTERVAL, unit.stableOrder(), ctx.nextScheduleSequence(), unit.seed.id(), null, passive.id(), 0));
                }
            }
            ctx.initializeCooldowns(unit);
            ctx.scheduleAction(unit, ctx.ruleset.attackIntervalMs(ctx.effectiveSpeed(unit)), 0);
        }

        long currentTimeMs = 0;
        while (!ctx.queue.isEmpty()) {
            ScheduledEvent scheduled = ctx.queue.poll();
            if (scheduled.timestampMs > ctx.ruleset.maxBattleDurationMs()) {
                currentTimeMs = ctx.ruleset.maxBattleDurationMs();
                break;
            }
            currentTimeMs = scheduled.timestampMs;
            ctx.currentTimeMs = currentTimeMs;
            ctx.processScheduled(scheduled);
            BattleOutcome terminal = ctx.terminalOutcome();
            if (terminal != null) {
                ctx.emit(currentTimeMs, BattleEventType.BATTLE_END, null, null, 0, false, null, null, null, 0, terminal.name(), null, 0, null);
                return ctx.result(terminal, currentTimeMs);
            }
        }

        BattleOutcome timeout = ctx.resolveTimeout();
        ctx.emit(currentTimeMs, BattleEventType.BATTLE_END, null, null, 0, false, null, null, null, 0, "TIMEOUT:" + timeout.name(), null, 0, null);
        return ctx.result(timeout, currentTimeMs);
    }

    private enum ScheduledType { ACTION_READY, CAST_COMPLETE, STATUS_TICK, STATUS_EXPIRE, PASSIVE_INTERVAL }

    private record ScheduledEvent(long timestampMs, ScheduledType type, String stableOrder, long sequence,
                                  String actorId, String targetId, String payload, long token) {}

    private static final class Context {
        private final long seed;
        private final BattleRuleset ruleset;
        private final Random random;
        private final Map<String, UnitState> units = new LinkedHashMap<>();
        private final List<BattleEvent> events = new ArrayList<>();
        private final PriorityQueue<ScheduledEvent> queue;
        private final Map<TeamSide, Long> damageDealt = new HashMap<>();
        private long scheduleSequence;
        private int eventSequence;
        private long currentTimeMs;

        private Context(long seed, BattleRuleset ruleset, List<BattleUnitSeed> seeds) {
            this.seed = seed;
            this.ruleset = ruleset;
            this.random = new Random(seed);
            this.queue = new PriorityQueue<>(Comparator
                    .comparingLong(ScheduledEvent::timestampMs)
                    .thenComparingInt(it -> scheduledPriority(it.type()))
                    .thenComparing(ScheduledEvent::stableOrder)
                    .thenComparingLong(ScheduledEvent::sequence));
            for (BattleUnitSeed seedUnit : seeds) {
                if (units.putIfAbsent(seedUnit.id(), new UnitState(seedUnit)) != null) throw new IllegalArgumentException("duplicate unit id: " + seedUnit.id());
            }
            if (units.values().stream().noneMatch(it -> it.seed.side() == TeamSide.A) || units.values().stream().noneMatch(it -> it.seed.side() == TeamSide.B)) {
                throw new IllegalArgumentException("both teams require at least one unit");
            }
        }

        private static int scheduledPriority(ScheduledType type) {
            return switch (type) {
                case STATUS_TICK -> 10;
                case STATUS_EXPIRE -> 20;
                case CAST_COMPLETE -> 30;
                case PASSIVE_INTERVAL -> 40;
                case ACTION_READY -> 50;
            };
        }

        private long nextScheduleSequence() { return ++scheduleSequence; }
        private void schedule(ScheduledEvent event) { queue.add(event); }

        private void initializeCooldowns(UnitState unit) {
            BattleAbilitySet abilities = unit.seed.abilities();
            unit.cooldownReadyAt.put(abilities.skill1().id(), abilities.skill1().cooldownMs());
            unit.cooldownReadyAt.put(abilities.skill2().id(), abilities.skill2().cooldownMs());
            unit.cooldownReadyAt.put(abilities.rageSkill().id(), 0L);
        }

        private void scheduleAction(UnitState unit, long timestampMs, long minimumTimestamp) {
            if (!unit.alive()) return;
            long when = Math.max(timestampMs, Math.max(minimumTimestamp, unit.actionLockUntilMs));
            long token = ++unit.actionGeneration;
            unit.nextActionAtMs = when;
            schedule(new ScheduledEvent(when, ScheduledType.ACTION_READY, unit.stableOrder(), nextScheduleSequence(), unit.seed.id(), null, null, token));
        }

        private void processScheduled(ScheduledEvent event) {
            UnitState actor = units.get(event.actorId());
            switch (event.type()) {
                case ACTION_READY -> {
                    if (actor == null || !actor.alive() || event.token() != actor.actionGeneration) return;
                    processActionReady(actor, event.timestampMs());
                }
                case CAST_COMPLETE -> {
                    if (actor == null || !actor.alive() || event.token() != actor.castGeneration || actor.castingAbility == null) return;
                    BattleAbility ability = actor.castingAbility;
                    actor.castingAbility = null;
                    emit(event.timestampMs(), BattleEventType.CAST_COMPLETE, actor.seed.id(), null, 0, false, ability.id(), ability.kind(), ability.effectKey(), actor.rage, null, null, 0, null);
                    executeAbility(actor, ability, event.timestampMs());
                }
                case STATUS_TICK -> processStatusTick(event);
                case STATUS_EXPIRE -> processStatusExpire(event);
                case PASSIVE_INTERVAL -> processPassiveInterval(actor, event);
            }
        }

        private void processActionReady(UnitState actor, long now) {
            expireLazy(actor, now);
            if (!actor.alive()) return;
            if (actor.hasStatus("STUN", now)) {
                long resume = Math.max(now + ruleset.aiDecisionIntervalMs(), actor.statuses.get("STUN").expiresAtMs);
                emit(now, BattleEventType.ACTION_BLOCKED, actor.seed.id(), null, 0, false, null, null, null, actor.rage, null, "STUN", Math.max(0, resume - now), null);
                scheduleAction(actor, resume, resume);
                return;
            }
            if (now < actor.actionLockUntilMs) {
                scheduleAction(actor, actor.actionLockUntilMs, actor.actionLockUntilMs);
                return;
            }

            emit(now, BattleEventType.ACTION_READY, actor.seed.id(), null, 0, false, null, null, null, actor.rage, null, null, 0, null);
            triggerPassives(PassiveTrigger.BEFORE_ACTION, actor, null, now, false);
            boolean silenced = actor.hasStatus("SILENCE", now);
            BattleAbility ability = chooseAbility(actor, now, silenced);
            if (ability == null) {
                scheduleAction(actor, now + ruleset.aiDecisionIntervalMs(), now);
                return;
            }
            if (ability.kind() != BattleAbilityKind.BASIC && ability.kind() != BattleAbilityKind.RAGE_SKILL) {
                actor.cooldownReadyAt.put(ability.id(), now + ability.cooldownMs());
            }
            if (ability.kind() == BattleAbilityKind.RAGE_SKILL) {
                emit(now, BattleEventType.RAGE_SKILL_READY, actor.seed.id(), null, 0, false, ability.id(), ability.kind(), ability.effectKey(), actor.rage, null, null, 0, null);
            }
            triggerPassives(ability.kind() == BattleAbilityKind.RAGE_SKILL ? PassiveTrigger.RAGE_SKILL_CAST : PassiveTrigger.SKILL_CAST,
                    actor, null, now, false);

            actor.actionLockUntilMs = now + ability.castTimeMs() + ability.recoveryMs();
            if (ability.castTimeMs() > 0) {
                actor.castingAbility = ability;
                long token = ++actor.castGeneration;
                BattleEventType type = ability.kind() == BattleAbilityKind.RAGE_SKILL ? BattleEventType.RAGE_SKILL_CAST_START : BattleEventType.CAST_START;
                emit(now, type, actor.seed.id(), null, 0, false, ability.id(), ability.kind(), ability.effectKey(), actor.rage, null, null, ability.castTimeMs(), null);
                schedule(new ScheduledEvent(now + ability.castTimeMs(), ScheduledType.CAST_COMPLETE, actor.stableOrder(), nextScheduleSequence(), actor.seed.id(), null, ability.id(), token));
            } else {
                executeAbility(actor, ability, now);
            }
            long next = now + ruleset.attackIntervalMs(effectiveSpeed(actor));
            scheduleAction(actor, Math.max(next, actor.actionLockUntilMs), actor.actionLockUntilMs);
            triggerPassives(PassiveTrigger.AFTER_ACTION, actor, null, now, false);
        }

        private BattleAbility chooseAbility(UnitState actor, long now, boolean silenced) {
            BattleAbilitySet set = actor.seed.abilities();
            if (!silenced && actor.rage >= ruleset.rageSkillCost()) return set.rageSkill();
            if (!silenced && cooldownReady(actor, set.skill2(), now)) return set.skill2();
            if (!silenced && cooldownReady(actor, set.skill1(), now)) return set.skill1();
            return set.basic();
        }

        private boolean cooldownReady(UnitState actor, BattleAbility ability, long now) {
            return now >= actor.cooldownReadyAt.getOrDefault(ability.id(), 0L);
        }

        private void executeAbility(UnitState actor, BattleAbility ability, long now) {
            if (!actor.alive()) return;
            BattleEventType opening = ability.kind() == BattleAbilityKind.BASIC ? BattleEventType.BASIC_ATTACK_START : BattleEventType.ABILITY;
            emit(now, opening, actor.seed.id(), null, 0, false, ability.id(), ability.kind(), ability.effectKey(), actor.rage, null, null, 0, null);
            for (SkillEffectDefinition effect : ability.effects()) {
                List<UnitState> targets = resolveTargets(actor, effect.target(), effect.type());
                for (UnitState target : targets) applyEffect(actor, target, ability, effect, now, false);
            }
            int delta = ability.rageDelta();
            if (ability.kind() == BattleAbilityKind.BASIC && delta == 0) delta = ruleset.defaultBasicRageGain();
            if (delta != 0) changeRage(actor, delta, now, ability);
            if (ability.kind() == BattleAbilityKind.BASIC) {
                emit(now, BattleEventType.BASIC_ATTACK_END, actor.seed.id(), null, 0, false, ability.id(), ability.kind(), ability.effectKey(), actor.rage, null, null, 0, null);
            } else if (ability.kind() == BattleAbilityKind.RAGE_SKILL) {
                emit(now, BattleEventType.RAGE_SKILL_CAST_END, actor.seed.id(), null, 0, false, ability.id(), ability.kind(), ability.effectKey(), actor.rage, null, null, 0, null);
            }
        }

        private void applyEffect(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect, long now, boolean passiveChain) {
            if (target == null) return;
            switch (effect.type()) {
                case DAMAGE -> applyDamage(source, target, ability, effect, now, passiveChain);
                case HEAL -> applyHeal(source, target, ability, effect, now);
                case SHIELD -> applyShield(source, target, ability, effect, now);
                case RAGE, ENERGY -> changeRage(target, (int) Math.min(Integer.MAX_VALUE, effect.flatAmount()), now, ability);
                case STATUS -> applyStatus(source, target, ability, effect, now, passiveChain);
                case CLEANSE -> removeStatuses(source, target, true, now, BattleEventType.STATUS_CLEANSED);
                case DISPEL -> removeStatuses(source, target, false, now, BattleEventType.STATUS_REMOVED);
                case REVIVE -> applyRevive(source, target, ability, effect, now);
            }
        }

        private void applyDamage(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect, long now, boolean passiveChain) {
            if (!source.alive() || !target.alive()) return;
            if (!passiveChain) triggerPassives(PassiveTrigger.BEFORE_DAMAGE, source, target, now, true);
            DamageRoll roll = damage(source, target, ability, effect);
            long remaining = roll.amount;
            if (target.shield > 0) {
                long absorbed = Math.min(target.shield, remaining);
                target.shield -= absorbed;
                remaining -= absorbed;
                emit(now, BattleEventType.SHIELD_ABSORB, source.seed.id(), target.seed.id(), absorbed, false, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), null, 0, null);
            }
            long applied = Math.min(target.hp, Math.max(0, remaining));
            target.hp -= applied;
            source.damageDealt += applied;
            damageDealt.merge(source.seed.side(), applied, Long::sum);
            BattleEventType hitType = ability.kind() == BattleAbilityKind.BASIC ? BattleEventType.BASIC_ATTACK_HIT
                    : ability.kind() == BattleAbilityKind.RAGE_SKILL ? BattleEventType.RAGE_SKILL_HIT : BattleEventType.DAMAGE;
            emit(now, hitType, source.seed.id(), target.seed.id(), applied, roll.critical, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), null, 0, null);
            if (hitType != BattleEventType.DAMAGE) {
                emit(now, BattleEventType.DAMAGE, source.seed.id(), target.seed.id(), applied, roll.critical, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), null, 0, null);
            }
            if (roll.critical && !passiveChain) triggerPassives(PassiveTrigger.CRITICAL, source, target, now, true);
            if (!passiveChain) {
                triggerPassives(PassiveTrigger.AFTER_DAMAGE_DEALT, source, target, now, true);
                triggerPassives(PassiveTrigger.AFTER_DAMAGE_TAKEN, target, source, now, true);
                triggerHpThreshold(target, now);
            }
            if (target.hp <= 0) handleKo(source, target, now, passiveChain);
        }

        private void applyHeal(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect, long now) {
            if (!target.alive()) return;
            long amount = scaledAmount(source, effect, ability);
            long applied = Math.min(amount, target.seed.maxHp() - target.hp);
            if (applied <= 0) return;
            target.hp += applied;
            emit(now, BattleEventType.HEAL, source.seed.id(), target.seed.id(), applied, false, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), null, 0, null);
        }

        private void applyShield(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect, long now) {
            if (!target.alive()) return;
            long amount = scaledAmount(source, effect, ability);
            target.shield = Math.addExact(target.shield, amount);
            emit(now, BattleEventType.SHIELD, source.seed.id(), target.seed.id(), amount, false, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), null, effect.durationMs(), null);
        }

        private void applyRevive(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect, long now) {
            if (target.alive()) return;
            long coefficient = effect.coefficientBps() > 0 ? effect.coefficientBps() : 3_000;
            long hp = Math.max(1, target.seed.maxHp() * coefficient / 10_000 + effect.flatAmount());
            target.hp = Math.min(target.seed.maxHp(), hp);
            target.shield = 0;
            target.statuses.clear();
            target.rage = 0;
            emit(now, BattleEventType.REVIVE, source.seed.id(), target.seed.id(), target.hp, false, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), null, 0, null);
            scheduleAction(target, now + ruleset.attackIntervalMs(effectiveSpeed(target)), now);
        }

        private void applyStatus(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect, long now, boolean passiveChain) {
            if (!target.alive() || effect.status() == null || effect.status().isBlank()) return;
            if (effect.chanceBps() < 10_000 && random.nextInt(10_000) >= effect.chanceBps()) return;
            if (effect.durationMs() <= 0) return;
            long expiresAt = now + effect.durationMs();
            long tickAmount = 0;
            if (effect.tickIntervalMs() > 0 || DOT_STATUSES.contains(effect.status())) tickAmount = scaledAmount(source, effect, ability);
            long interval = effect.tickIntervalMs();
            StatusState state = new StatusState(effect.status(), source.seed.id(), now, expiresAt,
                    interval > 0 ? now + interval : 0, interval, tickAmount, effect.coefficientBps(), NEGATIVE_STATUSES.contains(effect.status()));
            target.statuses.put(effect.status(), state);
            emit(now, BattleEventType.STATUS_APPLIED, source.seed.id(), target.seed.id(), 0, false, ability.id(), ability.kind(), ability.effectKey(), source.rage, effect.type().name(), effect.status(), effect.durationMs(), null);
            if ("STUN".equals(effect.status()) && target.castingAbility != null) {
                BattleAbility interrupted = target.castingAbility;
                target.castingAbility = null;
                target.castGeneration++;
                emit(now, BattleEventType.INTERRUPT, source.seed.id(), target.seed.id(), 0, false, interrupted.id(), interrupted.kind(), interrupted.effectKey(), target.rage, null, "STUN", effect.durationMs(), null);
            }
            if (interval > 0) {
                schedule(new ScheduledEvent(now + interval, ScheduledType.STATUS_TICK, target.stableOrder(), nextScheduleSequence(), source.seed.id(), target.seed.id(), effect.status(), state.appliedAtMs));
            }
            schedule(new ScheduledEvent(expiresAt, ScheduledType.STATUS_EXPIRE, target.stableOrder(), nextScheduleSequence(), source.seed.id(), target.seed.id(), effect.status(), state.appliedAtMs));
            if ("SPEED_UP".equals(effect.status()) || "SPEED_DOWN".equals(effect.status())) rescheduleForSpeedChange(target, now);
            if (!passiveChain) triggerPassives(PassiveTrigger.STATUS_APPLIED, target, source, now, true);
        }

        private void processStatusTick(ScheduledEvent event) {
            UnitState target = units.get(event.targetId());
            UnitState source = units.get(event.actorId());
            if (target == null || source == null || !target.alive()) return;
            StatusState state = target.statuses.get(event.payload());
            if (state == null || state.appliedAtMs != event.token() || event.timestampMs() > state.expiresAtMs) return;
            long applied = Math.min(target.hp, Math.max(0, state.tickAmount));
            target.hp -= applied;
            source.damageDealt += applied;
            damageDealt.merge(source.seed.side(), applied, Long::sum);
            emit(event.timestampMs(), BattleEventType.STATUS_TICK, source.seed.id(), target.seed.id(), applied, false, null, null, null, target.rage, "STATUS", state.statusId, Math.max(0, state.expiresAtMs - event.timestampMs()), null);
            if (target.hp <= 0) handleKo(source, target, event.timestampMs(), true);
            long next = event.timestampMs() + state.tickIntervalMs;
            if (state.tickIntervalMs > 0 && next <= state.expiresAtMs && target.alive()) {
                schedule(new ScheduledEvent(next, ScheduledType.STATUS_TICK, target.stableOrder(), nextScheduleSequence(), source.seed.id(), target.seed.id(), state.statusId, state.appliedAtMs));
            }
        }

        private void processStatusExpire(ScheduledEvent event) {
            UnitState target = units.get(event.targetId());
            if (target == null) return;
            StatusState state = target.statuses.get(event.payload());
            if (state == null || state.appliedAtMs != event.token()) return;
            target.statuses.remove(event.payload());
            emit(event.timestampMs(), BattleEventType.STATUS_EXPIRED, event.actorId(), target.seed.id(), 0, false, null, null, null, target.rage, null, state.statusId, 0, null);
            if ("SPEED_UP".equals(state.statusId) || "SPEED_DOWN".equals(state.statusId)) rescheduleForSpeedChange(target, event.timestampMs());
        }

        private void expireLazy(UnitState unit, long now) {
            List<String> expired = unit.statuses.values().stream().filter(it -> now >= it.expiresAtMs).map(it -> it.statusId).toList();
            for (String id : expired) unit.statuses.remove(id);
        }

        private void removeStatuses(UnitState source, UnitState target, boolean negative, long now, BattleEventType eventType) {
            List<String> ids = target.statuses.values().stream().filter(it -> it.negative == negative).map(it -> it.statusId).toList();
            for (String id : ids) {
                StatusState removed = target.statuses.remove(id);
                emit(now, eventType, source.seed.id(), target.seed.id(), 0, false, null, null, null, target.rage, null, id, 0, null);
                if (removed != null && ("SPEED_UP".equals(id) || "SPEED_DOWN".equals(id))) rescheduleForSpeedChange(target, now);
            }
        }

        private void processPassiveInterval(UnitState actor, ScheduledEvent event) {
            if (actor == null || !actor.alive()) return;
            BattlePassive passive = actor.seed.passives().stream().filter(it -> it.id().equals(event.payload()) && it.trigger() == PassiveTrigger.TIME_INTERVAL).findFirst().orElse(null);
            if (passive == null) return;
            triggerPassive(actor, passive, null, event.timestampMs(), true);
            schedule(new ScheduledEvent(event.timestampMs() + passive.intervalMs(), ScheduledType.PASSIVE_INTERVAL, actor.stableOrder(), nextScheduleSequence(), actor.seed.id(), null, passive.id(), 0));
        }

        private void triggerHpThreshold(UnitState unit, long now) {
            for (BattlePassive passive : unit.seed.passives()) {
                if (passive.trigger() != PassiveTrigger.HP_THRESHOLD || passive.thresholdBps() <= 0) continue;
                long hpBps = unit.hp * 10_000 / unit.seed.maxHp();
                if (hpBps <= passive.thresholdBps()) triggerPassive(unit, passive, null, now, true);
            }
        }

        private void triggerPassives(PassiveTrigger trigger, UnitState owner, UnitState contextTarget, long now, boolean reactionChain) {
            for (BattlePassive passive : owner.seed.passives()) {
                if (passive.trigger() == trigger) triggerPassive(owner, passive, contextTarget, now, reactionChain);
            }
        }

        private void triggerPassive(UnitState owner, BattlePassive passive, UnitState contextTarget, long now, boolean reactionChain) {
            if (!owner.alive()) return;
            if (passive.oncePerBattle() && owner.firedPassives.contains(passive.id())) return;
            if (passive.oncePerBattle()) owner.firedPassives.add(passive.id());
            emit(now, BattleEventType.PASSIVE_TRIGGER, owner.seed.id(), contextTarget == null ? null : contextTarget.seed.id(), 0, false,
                    passive.id(), null, "vfx/passives/" + passive.id(), owner.rage, null, null, 0, passive.trigger().name());
            BattleAbility wrapper = new BattleAbility(passive.id(), BattleAbilityKind.BASIC, owner.seed.primaryChannel(), 10_000, 0,
                    "vfx/passives/" + passive.id(), passive.effects(), 0, 0, 0);
            for (SkillEffectDefinition effect : passive.effects()) {
                for (UnitState target : resolveTargets(owner, effect.target(), effect.type())) applyEffect(owner, target, wrapper, effect, now, true);
            }
        }

        private void handleKo(UnitState killer, UnitState target, long now, boolean passiveChain) {
            if (target.koEmitted) return;
            target.koEmitted = true;
            target.castingAbility = null;
            target.castGeneration++;
            emit(now, BattleEventType.KO, killer == null ? null : killer.seed.id(), target.seed.id(), 0, false, null, null, null, killer == null ? 0 : killer.rage, null, null, 0, null);
            if (!passiveChain) {
                for (UnitState ally : units.values()) if (ally.alive() && ally.seed.side() == target.seed.side() && ally != target) triggerPassives(PassiveTrigger.ALLY_KO, ally, target, now, true);
                for (UnitState enemy : units.values()) if (enemy.alive() && enemy.seed.side() != target.seed.side()) triggerPassives(PassiveTrigger.ENEMY_KO, enemy, target, now, true);
            }
        }

        private void changeRage(UnitState target, int delta, long now, BattleAbility ability) {
            int before = target.rage;
            target.rage = Math.max(0, Math.min(MAX_RAGE, target.rage + delta));
            int changed = target.rage - before;
            if (changed == 0) return;
            emit(now, BattleEventType.RAGE_GAIN, target.seed.id(), target.seed.id(), changed, false,
                    ability == null ? null : ability.id(), ability == null ? null : ability.kind(), ability == null ? null : ability.effectKey(), target.rage, "RAGE", null, 0, null);
            if (before < MAX_RAGE && target.rage == MAX_RAGE) {
                emit(now, BattleEventType.RAGE_FULL, target.seed.id(), target.seed.id(), 0, false,
                        target.seed.abilities().rageSkill().id(), BattleAbilityKind.RAGE_SKILL, target.seed.abilities().rageSkill().effectKey(), target.rage, "RAGE", null, 0, null);
            }
        }

        private void rescheduleForSpeedChange(UnitState target, long now) {
            if (!target.alive() || target.castingAbility != null) return;
            long next = now + ruleset.attackIntervalMs(effectiveSpeed(target));
            scheduleAction(target, next, now);
        }

        private List<UnitState> resolveTargets(UnitState actor, TargetSelector selector, EffectType effectType) {
            List<UnitState> alliesLiving = units.values().stream().filter(UnitState::alive).filter(it -> it.seed.side() == actor.seed.side()).sorted(UnitState.ORDER).toList();
            List<UnitState> enemiesLiving = units.values().stream().filter(UnitState::alive).filter(it -> it.seed.side() != actor.seed.side()).sorted(UnitState.ORDER).toList();
            return switch (selector) {
                case SELF -> List.of(actor);
                case FRONTMOST_ENEMY -> enemiesLiving.isEmpty() ? List.of() : List.of(enemiesLiving.get(0));
                case LOWEST_HP_ENEMY -> pickLowestHp(enemiesLiving);
                case RANDOM_ENEMY -> enemiesLiving.isEmpty() ? List.of() : List.of(enemiesLiving.get(random.nextInt(enemiesLiving.size())));
                case ALL_ENEMIES -> enemiesLiving;
                case LOWEST_HP_ALLY -> {
                    if (effectType == EffectType.REVIVE) {
                        List<UnitState> dead = units.values().stream().filter(it -> !it.alive()).filter(it -> it.seed.side() == actor.seed.side()).sorted(UnitState.ORDER).toList();
                        yield dead.isEmpty() ? pickLowestHp(alliesLiving) : List.of(dead.get(0));
                    }
                    yield pickLowestHp(alliesLiving);
                }
                case ALL_ALLIES -> alliesLiving;
            };
        }

        private List<UnitState> pickLowestHp(List<UnitState> candidates) {
            return candidates.stream().min(Comparator.<UnitState>comparingDouble(it -> (double) it.hp / it.seed.maxHp()).thenComparing(UnitState.ORDER)).map(List::of).orElseGet(List::of);
        }

        private DamageRoll damage(UnitState source, UnitState target, BattleAbility ability, SkillEffectDefinition effect) {
            DamageChannel channel = effect.channel() == null ? ability.channel() : effect.channel();
            long attack = effectiveAttack(source, channel);
            long defense = effectiveDefense(target, channel);
            int coefficient = effect.coefficientBps() > 0 ? effect.coefficientBps() : ability.coefficientBps();
            long raw = Math.max(1, attack * coefficient / 10_000 + effect.flatAmount());
            long mitigated = Math.max(1, raw * ruleset.flatDefenseScale() / (ruleset.flatDefenseScale() + defense));
            int variance = ruleset.varianceBps() == 0 ? 0 : random.nextInt(ruleset.varianceBps() * 2 + 1) - ruleset.varianceBps();
            long amount = Math.max(1, mitigated * (10_000L + variance) / 10_000L);
            int unitCrit = channel == DamageChannel.PHYSICAL ? source.seed.physicalCritBps() : source.seed.chakraCritBps();
            boolean critical = random.nextInt(10_000) < Math.min(10_000, ruleset.critChanceBps() + unitCrit);
            if (critical) amount = Math.max(1, amount * 15_000L / 10_000L);
            return new DamageRoll(amount, critical);
        }

        private long scaledAmount(UnitState source, SkillEffectDefinition effect, BattleAbility ability) {
            DamageChannel channel = effect.channel() == null ? ability.channel() : effect.channel();
            long attack = effectiveAttack(source, channel);
            int coefficient = effect.coefficientBps() > 0 ? effect.coefficientBps() : ability.coefficientBps();
            return Math.max(0, attack * coefficient / 10_000 + effect.flatAmount());
        }

        private long effectiveAttack(UnitState unit, DamageChannel channel) {
            long base = channel == DamageChannel.PHYSICAL ? unit.seed.physicalAttack() : unit.seed.chakraAttack();
            int modifier = statusModifier(unit, "ATK_UP", "ATK_DOWN");
            return Math.max(0, base * Math.max(0, 10_000L + modifier) / 10_000L);
        }

        private long effectiveDefense(UnitState unit, DamageChannel channel) {
            long base = channel == DamageChannel.PHYSICAL ? unit.seed.physicalDefense() : unit.seed.chakraDefense();
            int modifier = statusModifier(unit, "DEF_UP", "DEF_DOWN");
            return Math.max(0, base * Math.max(0, 10_000L + modifier) / 10_000L);
        }

        private int effectiveSpeed(UnitState unit) {
            int modifier = statusModifier(unit, "SPEED_UP", "SPEED_DOWN");
            return (int) Math.max(1, unit.seed.speed() * Math.max(1, 10_000L + modifier) / 10_000L);
        }

        private int statusModifier(UnitState unit, String positive, String negative) {
            int value = 0;
            StatusState up = unit.statuses.get(positive);
            if (up != null && currentTimeMs < up.expiresAtMs) value += up.modifierBps;
            StatusState down = unit.statuses.get(negative);
            if (down != null && currentTimeMs < down.expiresAtMs) value -= down.modifierBps;
            return value;
        }

        private BattleOutcome terminalOutcome() {
            boolean a = units.values().stream().anyMatch(it -> it.alive() && it.seed.side() == TeamSide.A);
            boolean b = units.values().stream().anyMatch(it -> it.alive() && it.seed.side() == TeamSide.B);
            if (a && b) return null;
            if (a) return BattleOutcome.TEAM_A;
            if (b) return BattleOutcome.TEAM_B;
            return BattleOutcome.DRAW;
        }

        private BattleOutcome resolveTimeout() {
            int aliveA = living(TeamSide.A), aliveB = living(TeamSide.B);
            if (aliveA != aliveB) return aliveA > aliveB ? BattleOutcome.TEAM_A : BattleOutcome.TEAM_B;
            long hpA = hpRatioScore(TeamSide.A), hpB = hpRatioScore(TeamSide.B);
            if (hpA != hpB) return hpA > hpB ? BattleOutcome.TEAM_A : BattleOutcome.TEAM_B;
            long dmgA = damageDealt.getOrDefault(TeamSide.A, 0L), dmgB = damageDealt.getOrDefault(TeamSide.B, 0L);
            if (dmgA != dmgB) return dmgA > dmgB ? BattleOutcome.TEAM_A : BattleOutcome.TEAM_B;
            return BattleOutcome.DRAW;
        }

        private int living(TeamSide side) { return (int) units.values().stream().filter(it -> it.alive() && it.seed.side() == side).count(); }
        private long hpRatioScore(TeamSide side) { return units.values().stream().filter(it -> it.seed.side() == side).mapToLong(it -> it.hp * 10_000L / it.seed.maxHp()).sum(); }

        private void emit(long timestampMs, BattleEventType type, String actorId, String targetId, long amount, boolean critical,
                          String abilityId, BattleAbilityKind abilityKind, String effectKey, int rageAfter,
                          String effectType, String statusId, long durationMs, String triggerId) {
            events.add(new BattleEvent(eventSequence++, timestampMs, type, actorId, targetId, amount, critical,
                    abilityId, abilityKind, effectKey, Math.max(0, Math.min(MAX_RAGE, rageAfter)), effectType, statusId, durationMs, triggerId));
        }

        private BattleResult result(BattleOutcome outcome, long durationMs) {
            Map<String, Long> finalHp = new LinkedHashMap<>();
            units.forEach((id, state) -> finalHp.put(id, state.hp));
            return new BattleResult(seed, ruleset.version(), outcome, durationMs, List.copyOf(events), Map.copyOf(finalHp));
        }
    }

    private static final class UnitState {
        private static final Comparator<UnitState> ORDER = Comparator.comparingInt((UnitState it) -> it.seed.slot()).thenComparing(it -> it.seed.id());
        private final BattleUnitSeed seed;
        private final Map<String, StatusState> statuses = new HashMap<>();
        private final Map<String, Long> cooldownReadyAt = new HashMap<>();
        private final Set<String> firedPassives = new HashSet<>();
        private long hp;
        private long shield;
        private int rage;
        private long nextActionAtMs;
        private long actionLockUntilMs;
        private long actionGeneration;
        private long castGeneration;
        private BattleAbility castingAbility;
        private boolean koEmitted;
        private long damageDealt;

        private UnitState(BattleUnitSeed seed) { this.seed = seed; this.hp = seed.maxHp(); }
        private boolean alive() { return hp > 0; }
        private boolean hasStatus(String status, long now) { StatusState state = statuses.get(status); return state != null && now < state.expiresAtMs; }
        private String stableOrder() { return seed.side().ordinal() + ":" + String.format("%02d", seed.slot()) + ":" + seed.id(); }
    }

    private record StatusState(String statusId, String sourceId, long appliedAtMs, long expiresAtMs, long nextTickAtMs,
                               long tickIntervalMs, long tickAmount, int modifierBps, boolean negative) {}
    private record DamageRoll(long amount, boolean critical) {}
}
