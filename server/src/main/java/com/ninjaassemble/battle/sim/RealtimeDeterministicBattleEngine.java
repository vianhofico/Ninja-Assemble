package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Deterministic continuous-time auto battle simulator.
 *
 * <p>Every combatant advances on an independent action timeline. Work is processed by one deterministic
 * priority queue ordered by timestamp, event priority, stable actor order and insertion sequence. No thread,
 * coroutine or wall-clock sleep participates in simulation state, so the same seed and input always produce
 * the same replay.</p>
 */
public final class RealtimeDeterministicBattleEngine {
    private static final Set<String> DOT_STATUSES = Set.of("BURN", "POISON", "BLEED");
    private static final Set<String> CONTROL_STATUSES = Set.of("STUN", "SILENCE");

    public RealtimeBattleResult simulate(BattleRequest request) {
        return simulate(request, RealtimeBattleRuleset.experimentalV1());
    }

    public RealtimeBattleResult simulate(BattleRequest request, RealtimeBattleRuleset rules) {
        if (request == null) throw new IllegalArgumentException("battle request required");
        if (rules == null) throw new IllegalArgumentException("realtime ruleset required");

        Simulation simulation = new Simulation(request, rules);
        simulation.start();
        return simulation.run();
    }

    private static final class Simulation {
        private final BattleRequest request;
        private final RealtimeBattleRuleset rules;
        private final SplittableRandom random;
        private final List<State> states;
        private final Map<String, State> statesById;
        private final Map<String, Integer> actorOrder;
        private final PriorityQueue<ScheduledEvent> queue = new PriorityQueue<>(ScheduledEvent.ORDER);
        private final EventSink sink = new EventSink();
        private long scheduleSequence;
        private long nowMs;

        private Simulation(BattleRequest request, RealtimeBattleRuleset rules) {
            this.request = request;
            this.rules = rules;
            this.random = new SplittableRandom(request.seed());
            this.states = request.units().stream()
                    .map(State::new)
                    .sorted(Comparator.comparing(State::side).thenComparingInt(State::slot).thenComparing(State::id))
                    .toList();
            this.statesById = new LinkedHashMap<>();
            this.actorOrder = new HashMap<>();
            for (int i = 0; i < states.size(); i++) {
                State state = states.get(i);
                if (statesById.put(state.id(), state) != null) throw new IllegalArgumentException("duplicate battle unit id: " + state.id());
                actorOrder.put(state.id(), i);
            }
        }

        private void start() {
            sink.emit(BattleEventType.BATTLE_START, 0L, null, null, 0, false,
                    null, null, null, -1, null, null, 0L, null);

            for (State owner : states) {
                triggerPassives(owner, PassiveTrigger.BATTLE_START, 0L, false);
            }
            for (State state : states) {
                if (state.alive()) scheduleReady(state, rules.actionIntervalMs(state.speed()));
            }
        }

        private RealtimeBattleResult run() {
            while (!queue.isEmpty() && winner(states) == null) {
                ScheduledEvent scheduled = queue.poll();
                if (scheduled.timestampMs() > rules.maxBattleDurationMs()) break;
                nowMs = scheduled.timestampMs();
                switch (scheduled.kind()) {
                    case STATUS_TICK -> processStatusTick(scheduled);
                    case STATUS_EXPIRE -> processStatusExpire(scheduled);
                    case CAST_COMPLETE -> processCastComplete(scheduled);
                    case ACTOR_READY -> processActorReady(scheduled);
                }
            }

            TeamSide winningSide = winner(states);
            long endTime = winningSide == null ? rules.maxBattleDurationMs() : nowMs;
            BattleOutcome outcome = winningSide == TeamSide.A
                    ? BattleOutcome.TEAM_A
                    : winningSide == TeamSide.B ? BattleOutcome.TEAM_B : BattleOutcome.DRAW;
            sink.emit(BattleEventType.BATTLE_END, endTime, null, null, 0, false,
                    null, null, null, -1, null, null, 0L, null);

            Map<String, Long> finalHp = new LinkedHashMap<>();
            for (State state : states) finalHp.put(state.id(), state.hp);
            return new RealtimeBattleResult(request.seed(), rules.version(), outcome, endTime, sink.events, finalHp);
        }

        private void processActorReady(ScheduledEvent event) {
            State actor = statesById.get(event.actorId());
            if (actor == null || !actor.alive() || event.token() != actor.readyGeneration) return;

            sink.emit(BattleEventType.ACTION_READY, nowMs, actor.id(), actor.id(), 0, false,
                    null, null, null, actor.energy, null, null, 0L, null);
            triggerPassives(actor, PassiveTrigger.TURN_START, nowMs, false);
            triggerPassives(actor, PassiveTrigger.SELF_LOW_HP, nowMs, false);
            if (!actor.alive()) return;

            if (actor.hasStatus("STUN", nowMs)) {
                sink.emit(BattleEventType.TURN_SKIPPED, nowMs, actor.id(), actor.id(), 0, false,
                        null, null, null, actor.energy, "STATUS", "STUN", actor.statusRemainingMs("STUN", nowMs), null);
                scheduleReady(actor, Math.addExact(nowMs, rules.actionIntervalMs(actor.speed())));
                return;
            }

            BattleAbility ability = actor.nextAbility(actor.hasStatus("SILENCE", nowMs), nowMs);
            State primaryTarget = firstTarget(ability.effects().isEmpty() ? List.of(fallbackDamage(ability)) : ability.effects(), actor, false);
            int energyAfter = actor.applyEnergy(ability.energyDelta());
            actor.markAbilityUsed(ability, nowMs);
            long castToken = actor.beginCast();

            sink.emit(BattleEventType.CAST_START, nowMs, actor.id(), primaryTarget == null ? null : primaryTarget.id(), 0, false,
                    ability.id(), ability.kind(), ability.effectKey(), energyAfter, null, null, ability.castTimeMs(), null);
            schedule(ScheduledKind.CAST_COMPLETE, rules.quantizeUp(Math.addExact(nowMs, ability.castTimeMs())), actor, ability, null, castToken);
        }

        private void processCastComplete(ScheduledEvent event) {
            State actor = statesById.get(event.actorId());
            BattleAbility ability = event.ability();
            if (actor == null || ability == null || !actor.alive() || event.token() != actor.castGeneration) return;

            if (actor.hasStatus("STUN", nowMs)) {
                sink.emit(BattleEventType.TURN_SKIPPED, nowMs, actor.id(), actor.id(), 0, false,
                        ability.id(), ability.kind(), ability.effectKey(), actor.energy, "STATUS", "STUN",
                        actor.statusRemainingMs("STUN", nowMs), null);
                scheduleAfterRecovery(actor, ability);
                return;
            }

            List<SkillEffectDefinition> effects = ability.effects().isEmpty() ? List.of(fallbackDamage(ability)) : ability.effects();
            State primaryTarget = firstTarget(effects, actor, false);
            sink.emit(BattleEventType.CAST_COMPLETE, nowMs, actor.id(), primaryTarget == null ? null : primaryTarget.id(), 0, false,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, null, null, 0L, null);
            sink.emit(BattleEventType.ATTACK, nowMs, actor.id(), primaryTarget == null ? null : primaryTarget.id(), 0, false,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, null, null, 0L, null);

            for (SkillEffectDefinition effect : effects) {
                applyEffect(effect, ability, actor, false);
                if (winner(states) != null) break;
            }
            if (actor.alive() && winner(states) == null) scheduleAfterRecovery(actor, ability);
        }

        private void scheduleAfterRecovery(State actor, BattleAbility ability) {
            long readyAt = Math.addExact(nowMs, Math.addExact(ability.recoveryMs(), rules.actionIntervalMs(actor.speed())));
            scheduleReady(actor, readyAt);
        }

        private void processStatusTick(ScheduledEvent event) {
            State target = statesById.get(event.actorId());
            if (target == null || !target.alive()) return;
            StatusState status = target.statuses.get(event.statusId());
            if (status == null || status.generation != event.token() || nowMs > status.expiresAtMs || status.tickAmount <= 0) return;

            DamageResult damage = target.damage(status.tickAmount);
            if (damage.shieldAbsorbed > 0) {
                sink.emit(BattleEventType.SHIELD_ABSORB, nowMs, status.sourceActorId, target.id(), damage.shieldAbsorbed, false,
                        status.abilityId, status.abilityKind, status.effectKey, target.energy, "STATUS", status.id,
                        Math.max(0L, status.expiresAtMs - nowMs), null);
            }
            sink.emit(BattleEventType.STATUS_TICK, nowMs, status.sourceActorId, target.id(), damage.hpDamage, false,
                    status.abilityId, status.abilityKind, status.effectKey, target.energy, "STATUS", status.id,
                    Math.max(0L, status.expiresAtMs - nowMs), null);

            State source = statesById.get(status.sourceActorId);
            if (damage.hpDamage > 0) {
                if (source != null && source.alive()) triggerPassives(source, PassiveTrigger.AFTER_DAMAGE_DEALT, nowMs, true);
                if (target.alive()) {
                    triggerPassives(target, PassiveTrigger.AFTER_DAMAGE_TAKEN, nowMs, true);
                    triggerPassives(target, PassiveTrigger.SELF_LOW_HP, nowMs, true);
                } else {
                    target.invalidateActions();
                    sink.emit(BattleEventType.KO, nowMs, status.sourceActorId, target.id(), 0, false,
                            status.abilityId, status.abilityKind, status.effectKey, target.energy, "STATUS", status.id, 0L, null);
                    triggerAllyKo(target, nowMs);
                }
            }

            long nextTick = rules.quantizeUp(Math.addExact(nowMs, status.tickIntervalMs));
            if (target.alive() && nextTick <= status.expiresAtMs) {
                schedule(ScheduledKind.STATUS_TICK, nextTick, target, null, status.id, status.generation);
            }
        }

        private void processStatusExpire(ScheduledEvent event) {
            State target = statesById.get(event.actorId());
            if (target == null) return;
            StatusState status = target.statuses.get(event.statusId());
            if (status == null || status.generation != event.token() || nowMs < status.expiresAtMs) return;
            target.statuses.remove(status.id);
            sink.emit(BattleEventType.STATUS_EXPIRED, nowMs, status.sourceActorId, target.id(), 0, false,
                    status.abilityId, status.abilityKind, status.effectKey, target.energy, "STATUS", status.id, 0L, null);
        }

        private void triggerPassives(State owner, PassiveTrigger trigger, long timestampMs, boolean passiveContext) {
            if (!owner.alive()) return;
            for (BattlePassive passive : owner.seed.passives()) {
                if (passive.trigger() != trigger) continue;
                if (passive.oncePerBattle() && owner.firedPassives.contains(passive.id())) continue;
                if (trigger == PassiveTrigger.SELF_LOW_HP) {
                    int threshold = passive.thresholdBps();
                    if (threshold <= 0 || owner.hp * 10_000L > owner.seed.maxHp() * (long) threshold) continue;
                }
                if (passive.oncePerBattle()) owner.firedPassives.add(passive.id());
                BattleAbility wrapper = new BattleAbility(
                        passive.id(), BattleAbilityKind.PASSIVE, owner.seed.primaryChannel(), 10_000, 0,
                        "vfx/passives/" + passive.id(), passive.effects(), 0L, 0L, 0L);
                sink.emit(BattleEventType.PASSIVE_TRIGGER, timestampMs, owner.id(), owner.id(), 0, false,
                        passive.id(), BattleAbilityKind.PASSIVE, wrapper.effectKey(), owner.energy,
                        "PASSIVE", null, 0L, trigger.name());
                for (SkillEffectDefinition effect : passive.effects()) {
                    applyEffect(effect, wrapper, owner, true);
                }
            }
        }

        private void triggerAllyKo(State defeated, long timestampMs) {
            states.stream()
                    .filter(State::alive)
                    .filter(it -> it.side() == defeated.side())
                    .filter(it -> it != defeated)
                    .sorted(Comparator.comparingInt(State::slot).thenComparing(State::id))
                    .forEach(owner -> triggerPassives(owner, PassiveTrigger.ALLY_KO, timestampMs, false));
        }

        private void applyEffect(SkillEffectDefinition effect, BattleAbility ability, State actor, boolean passiveContext) {
            List<State> resolved = targets(effect, actor, true);
            for (State target : resolved) {
                switch (effect.type()) {
                    case DAMAGE -> applyDamage(effect, ability, actor, target, passiveContext);
                    case HEAL -> applyHeal(effect, ability, actor, target);
                    case SHIELD -> applyShield(effect, ability, actor, target);
                    case ENERGY -> applyEnergy(effect, ability, actor, target);
                    case STATUS -> applyStatus(effect, ability, actor, target);
                    case CLEANSE -> applyCleanse(ability, actor, target, true);
                    case DISPEL -> applyCleanse(ability, actor, target, false);
                    case REVIVE -> applyRevive(effect, ability, actor, target);
                }
            }
        }

        private void applyDamage(SkillEffectDefinition effect, BattleAbility ability, State actor, State target, boolean passiveContext) {
            DamageChannel channel = effect.channel() == null ? ability.channel() : effect.channel();
            int coefficient = effect.coefficientBps() > 0 ? effect.coefficientBps() : ability.coefficientBps();
            boolean critical = rollCritical(random, actor.seed, channel);
            long damage = damage(actor, target, channel, coefficient, effect.flatAmount(), rules, critical);
            DamageResult applied = target.damage(damage);
            if (applied.shieldAbsorbed > 0) {
                sink.emit(BattleEventType.SHIELD_ABSORB, nowMs, actor.id(), target.id(), applied.shieldAbsorbed, critical,
                        ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0L, null);
            }
            sink.emit(BattleEventType.DAMAGE, nowMs, actor.id(), target.id(), applied.hpDamage, critical,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0L, null);

            if (!target.alive()) {
                target.invalidateActions();
                sink.emit(BattleEventType.KO, nowMs, actor.id(), target.id(), 0, false,
                        ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0L, null);
            }

            if (!passiveContext && applied.hpDamage > 0) {
                if (actor.alive()) triggerPassives(actor, PassiveTrigger.AFTER_DAMAGE_DEALT, nowMs, true);
                if (target.alive()) {
                    triggerPassives(target, PassiveTrigger.AFTER_DAMAGE_TAKEN, nowMs, true);
                    triggerPassives(target, PassiveTrigger.SELF_LOW_HP, nowMs, true);
                } else {
                    triggerAllyKo(target, nowMs);
                }
            }
        }

        private void applyHeal(SkillEffectDefinition effect, BattleAbility ability, State actor, State target) {
            if (!target.alive()) return;
            long requested = scaledAmount(actor, effect.channel() == null ? ability.channel() : effect.channel(), effect.coefficientBps(), effect.flatAmount());
            long applied = target.heal(requested);
            sink.emit(BattleEventType.HEAL, nowMs, actor.id(), target.id(), applied, false,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0L, null);
        }

        private void applyShield(SkillEffectDefinition effect, BattleAbility ability, State actor, State target) {
            if (!target.alive()) return;
            long amount = scaledAmount(actor, effect.channel() == null ? ability.channel() : effect.channel(), effect.coefficientBps(), effect.flatAmount());
            long applied = target.addShield(amount);
            sink.emit(BattleEventType.SHIELD, nowMs, actor.id(), target.id(), applied, false,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0L, null);
        }

        private void applyEnergy(SkillEffectDefinition effect, BattleAbility ability, State actor, State target) {
            int requested = Math.toIntExact(Math.max(-100, Math.min(100, effect.flatAmount())));
            int before = target.energy;
            int after = target.applyEnergy(requested);
            sink.emit(BattleEventType.ENERGY, nowMs, actor.id(), target.id(), after - before, false,
                    ability.id(), ability.kind(), ability.effectKey(), after, effect.type().name(), null, 0L, null);
        }

        private void applyStatus(SkillEffectDefinition effect, BattleAbility ability, State actor, State target) {
            if (!target.alive() || effect.status() == null || effect.status().isBlank()) return;
            int chance = effect.chanceBps() == 0 ? 10_000 : effect.chanceBps();
            if (random.nextInt(10_000) >= chance) return;

            String statusId = normalizeStatus(effect.status());
            long requestedDuration = effect.resolvedDurationMs(rules.legacyTurnDurationMs());
            long durationMs = rules.quantizeUp(Math.max(rules.simulationTickMs(), requestedDuration > 0 ? requestedDuration : rules.legacyTurnDurationMs()));
            long tickIntervalMs = rules.quantizeUp(Math.max(rules.simulationTickMs(), effect.resolvedTickIntervalMs(rules.defaultStatusTickIntervalMs())));
            long tickAmount = DOT_STATUSES.contains(statusId)
                    ? scaledAmount(actor, effect.channel() == null ? ability.channel() : effect.channel(), effect.coefficientBps(), effect.flatAmount())
                    : 0L;
            int modifierBps = DOT_STATUSES.contains(statusId) || CONTROL_STATUSES.contains(statusId) ? 0 : effect.coefficientBps();
            long expiresAtMs = rules.quantizeUp(Math.addExact(nowMs, durationMs));
            long generation = target.nextStatusGeneration();
            StatusState incoming = new StatusState(
                    statusId,
                    expiresAtMs,
                    tickIntervalMs,
                    tickAmount,
                    modifierBps,
                    isNegativeStatus(statusId),
                    generation,
                    actor.id(),
                    ability.id(),
                    ability.kind(),
                    ability.effectKey()
            );
            StatusState applied = target.applyStatus(incoming);
            schedule(ScheduledKind.STATUS_EXPIRE, applied.expiresAtMs, target, null, applied.id, applied.generation);
            if (applied.tickAmount > 0) {
                long firstTick = rules.quantizeUp(Math.addExact(nowMs, applied.tickIntervalMs));
                if (firstTick <= applied.expiresAtMs) schedule(ScheduledKind.STATUS_TICK, firstTick, target, null, applied.id, applied.generation);
            }
            sink.emit(BattleEventType.STATUS_APPLIED, nowMs, actor.id(), target.id(), 0, false,
                    ability.id(), ability.kind(), ability.effectKey(), target.energy, effect.type().name(), statusId,
                    Math.max(0L, applied.expiresAtMs - nowMs), null);
        }

        private void applyCleanse(BattleAbility ability, State actor, State target, boolean negative) {
            for (String removed : target.removeStatuses(negative)) {
                sink.emit(BattleEventType.STATUS_CLEANSED, nowMs, actor.id(), target.id(), 0, false,
                        ability.id(), ability.kind(), ability.effectKey(), target.energy,
                        negative ? EffectType.CLEANSE.name() : EffectType.DISPEL.name(), removed, 0L, null);
            }
        }

        private void applyRevive(SkillEffectDefinition effect, BattleAbility ability, State actor, State target) {
            if (target.alive()) return;
            long amount = effect.flatAmount() > 0
                    ? effect.flatAmount()
                    : Math.max(1, target.seed.maxHp() * Math.max(1, effect.coefficientBps()) / 10_000);
            long restored = target.revive(amount);
            sink.emit(BattleEventType.REVIVE, nowMs, actor.id(), target.id(), restored, false,
                    ability.id(), ability.kind(), ability.effectKey(), target.energy, effect.type().name(), null, 0L, null);
            scheduleReady(target, Math.addExact(nowMs, rules.actionIntervalMs(target.speed())));
        }

        private State firstTarget(List<SkillEffectDefinition> effects, State actor, boolean consumeRandom) {
            for (SkillEffectDefinition effect : effects) {
                List<State> resolved = targets(effect, actor, consumeRandom);
                if (!resolved.isEmpty()) return resolved.get(0);
            }
            return null;
        }

        private List<State> targets(SkillEffectDefinition effect, State actor, boolean consumeRandom) {
            boolean revive = effect.type() == EffectType.REVIVE;
            return switch (effect.target()) {
                case SELF -> List.of(actor);
                case FRONTMOST_ENEMY -> first(states.stream().filter(it -> it.side() != actor.side() && it.alive())
                        .sorted(Comparator.comparingInt(State::slot).thenComparing(State::id)).toList());
                case LOWEST_HP_ENEMY -> first(states.stream().filter(it -> it.side() != actor.side() && it.alive())
                        .sorted(Comparator.comparingDouble(State::hpRatio).thenComparing(State::id)).toList());
                case RANDOM_ENEMY -> {
                    List<State> values = states.stream().filter(it -> it.side() != actor.side() && it.alive())
                            .sorted(Comparator.comparing(State::id)).toList();
                    yield consumeRandom ? randomOne(values, random) : first(values);
                }
                case ALL_ENEMIES -> states.stream().filter(it -> it.side() != actor.side() && it.alive())
                        .sorted(Comparator.comparingInt(State::slot).thenComparing(State::id)).toList();
                case LOWEST_HP_ALLY -> first(states.stream().filter(it -> it.side() == actor.side() && (revive ? !it.alive() : it.alive()))
                        .sorted(Comparator.comparingDouble(State::hpRatio).thenComparing(State::id)).toList());
                case ALL_ALLIES -> states.stream().filter(it -> it.side() == actor.side() && (revive ? !it.alive() : it.alive()))
                        .sorted(Comparator.comparingInt(State::slot).thenComparing(State::id)).toList();
            };
        }

        private void scheduleReady(State actor, long timestampMs) {
            long token = actor.nextReadyGeneration();
            schedule(ScheduledKind.ACTOR_READY, rules.quantizeUp(timestampMs), actor, null, null, token);
        }

        private void schedule(ScheduledKind kind, long timestampMs, State actor, BattleAbility ability, String statusId, long token) {
            if (timestampMs > rules.maxBattleDurationMs()) return;
            queue.add(new ScheduledEvent(
                    timestampMs,
                    kind,
                    actorOrder.getOrDefault(actor.id(), Integer.MAX_VALUE),
                    scheduleSequence++,
                    actor.id(),
                    ability,
                    statusId,
                    token
            ));
        }
    }

    private enum ScheduledKind {
        STATUS_TICK(10),
        STATUS_EXPIRE(20),
        CAST_COMPLETE(30),
        ACTOR_READY(40);

        private final int priority;
        ScheduledKind(int priority) { this.priority = priority; }
    }

    private record ScheduledEvent(
            long timestampMs,
            ScheduledKind kind,
            int actorOrder,
            long sequence,
            String actorId,
            BattleAbility ability,
            String statusId,
            long token
    ) {
        private static final Comparator<ScheduledEvent> ORDER = Comparator
                .comparingLong(ScheduledEvent::timestampMs)
                .thenComparingInt(it -> it.kind.priority)
                .thenComparingInt(ScheduledEvent::actorOrder)
                .thenComparingLong(ScheduledEvent::sequence);
    }

    private static SkillEffectDefinition fallbackDamage(BattleAbility ability) {
        return new SkillEffectDefinition(EffectType.DAMAGE, TargetSelector.FRONTMOST_ENEMY, ability.channel(),
                ability.coefficientBps(), 0, null, 10_000, 0, 0L, 0L);
    }

    private static List<State> first(List<State> values) {
        return values.isEmpty() ? List.of() : List.of(values.get(0));
    }

    private static List<State> randomOne(List<State> values, SplittableRandom random) {
        return values.isEmpty() ? List.of() : List.of(values.get(random.nextInt(values.size())));
    }

    private static boolean rollCritical(SplittableRandom random, BattleUnitSeed actor, DamageChannel channel) {
        int chance = channel == DamageChannel.PHYSICAL ? actor.physicalCritBps() : actor.chakraCritBps();
        return random.nextInt(10_000) < chance;
    }

    private static long damage(State actor, State target, DamageChannel channel, int coefficientBps, long flatAmount,
                               RealtimeBattleRuleset rules, boolean critical) {
        long attack = actor.attack(channel);
        long defense = target.defense(channel);
        long raw = Math.max(1, Math.multiplyExact(attack, coefficientBps) / 10_000 + flatAmount);
        long mitigated = Math.max(1, raw * rules.defenseScale() / (rules.defenseScale() + Math.max(0, defense)));
        return critical ? Math.max(1, mitigated * rules.criticalMultiplierBps() / 10_000) : mitigated;
    }

    private static long scaledAmount(State actor, DamageChannel channel, int coefficientBps, long flatAmount) {
        long scaled = coefficientBps <= 0 ? 0 : Math.multiplyExact(actor.attack(channel), coefficientBps) / 10_000;
        return Math.max(0, scaled + flatAmount);
    }

    private static String normalizeStatus(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static boolean isNegativeStatus(String status) {
        return CONTROL_STATUSES.contains(status) || DOT_STATUSES.contains(status) || status.endsWith("_DOWN") || status.startsWith("DEBUFF_");
    }

    private static TeamSide winner(List<State> states) {
        boolean a = states.stream().anyMatch(it -> it.alive() && it.side() == TeamSide.A);
        boolean b = states.stream().anyMatch(it -> it.alive() && it.side() == TeamSide.B);
        if (a == b) return null;
        return a ? TeamSide.A : TeamSide.B;
    }

    private static final class EventSink {
        private final List<RealtimeBattleEvent> events = new ArrayList<>();
        private long sequence;

        private void emit(
                BattleEventType type,
                long timestampMs,
                String actorId,
                String targetId,
                long amount,
                boolean critical,
                String abilityId,
                BattleAbilityKind abilityKind,
                String effectKey,
                int energyAfter,
                String effectType,
                String statusId,
                long durationMs,
                String triggerId
        ) {
            events.add(new RealtimeBattleEvent(sequence++, timestampMs, type, actorId, targetId, amount, critical,
                    abilityId, abilityKind, effectKey, energyAfter, effectType, statusId, durationMs, triggerId));
        }
    }

    private static final class State {
        private final BattleUnitSeed seed;
        private final Map<String, StatusState> statuses = new LinkedHashMap<>();
        private final Set<String> firedPassives = new HashSet<>();
        private final Map<String, Long> cooldownReadyAtMs = new HashMap<>();
        private long hp;
        private long shield;
        private int energy;
        private int comboStep;
        private long readyGeneration;
        private long castGeneration;
        private long statusGeneration;

        private State(BattleUnitSeed seed) {
            this.seed = seed;
            this.hp = seed.maxHp();
        }

        private boolean alive() { return hp > 0; }
        private String id() { return seed.id(); }
        private TeamSide side() { return seed.side(); }
        private int slot() { return seed.slot(); }
        private double hpRatio() { return seed.maxHp() <= 0 ? 0 : (double) hp / seed.maxHp(); }
        private int speed() { return (int) Math.max(1, modified(seed.speed(), "SPEED")); }
        private long attack(DamageChannel channel) { return modified(channel == DamageChannel.PHYSICAL ? seed.physicalAttack() : seed.chakraAttack(), "ATK"); }
        private long defense(DamageChannel channel) { return modified(channel == DamageChannel.PHYSICAL ? seed.physicalDefense() : seed.chakraDefense(), "DEF"); }

        private long modified(long base, String stat) {
            int delta = 0;
            for (StatusState status : statuses.values()) {
                if (status.id.equals(stat + "_UP")) delta += status.modifierBps;
                if (status.id.equals(stat + "_DOWN")) delta -= status.modifierBps;
            }
            return Math.max(0, base * Math.max(0, 10_000L + delta) / 10_000L);
        }

        private BattleAbility nextAbility(boolean silenced, long nowMs) {
            BattleAbilitySet abilities = seed.abilities();
            if (silenced) return abilities.basic();
            if (energy >= 100 && ready(abilities.ultimate(), nowMs)) {
                comboStep = 0;
                return abilities.ultimate();
            }

            BattleAbility preferred = switch (comboStep) {
                case 0 -> abilities.basic();
                case 1 -> abilities.skill1();
                default -> abilities.skill2();
            };
            comboStep = (comboStep + 1) % 3;
            return ready(preferred, nowMs) ? preferred : abilities.basic();
        }

        private boolean ready(BattleAbility ability, long nowMs) {
            return cooldownReadyAtMs.getOrDefault(ability.id(), 0L) <= nowMs;
        }

        private void markAbilityUsed(BattleAbility ability, long nowMs) {
            cooldownReadyAtMs.put(ability.id(), Math.addExact(nowMs, ability.cooldownMs()));
        }

        private int applyEnergy(int delta) {
            energy = Math.max(0, Math.min(100, energy + delta));
            return energy;
        }

        private boolean hasStatus(String id, long nowMs) {
            StatusState value = statuses.get(id);
            return value != null && value.expiresAtMs >= nowMs;
        }

        private long statusRemainingMs(String id, long nowMs) {
            StatusState value = statuses.get(id);
            return value == null ? 0L : Math.max(0L, value.expiresAtMs - nowMs);
        }

        private StatusState applyStatus(StatusState incoming) {
            StatusState old = statuses.get(incoming.id);
            if (old == null) {
                statuses.put(incoming.id, incoming);
                return incoming;
            }
            StatusState stronger = StatusState.stronger(old, incoming);
            statuses.put(incoming.id, stronger);
            return stronger;
        }

        private List<String> removeStatuses(boolean negative) {
            List<String> removed = statuses.values().stream().filter(it -> it.negative == negative).map(it -> it.id).toList();
            removed.forEach(statuses::remove);
            return removed;
        }

        private DamageResult damage(long value) {
            long incoming = Math.max(0, value);
            long absorbed = Math.min(shield, incoming);
            shield -= absorbed;
            long remaining = incoming - absorbed;
            long hpDamage = Math.min(hp, remaining);
            hp -= hpDamage;
            return new DamageResult(hpDamage, absorbed);
        }

        private long heal(long value) {
            long applied = Math.min(seed.maxHp() - hp, Math.max(0, value));
            hp += applied;
            return applied;
        }

        private long addShield(long value) {
            long applied = Math.max(0, value);
            shield = Math.addExact(shield, applied);
            return applied;
        }

        private long revive(long value) {
            if (alive()) return 0;
            hp = Math.min(seed.maxHp(), Math.max(1, value));
            statuses.clear();
            shield = 0;
            invalidateActions();
            return hp;
        }

        private long nextReadyGeneration() { return ++readyGeneration; }
        private long beginCast() { return ++castGeneration; }
        private long nextStatusGeneration() { return ++statusGeneration; }

        private void invalidateActions() {
            readyGeneration++;
            castGeneration++;
        }
    }

    private static final class StatusState {
        private final String id;
        private final long expiresAtMs;
        private final long tickIntervalMs;
        private final long tickAmount;
        private final int modifierBps;
        private final boolean negative;
        private final long generation;
        private final String sourceActorId;
        private final String abilityId;
        private final BattleAbilityKind abilityKind;
        private final String effectKey;

        private StatusState(
                String id,
                long expiresAtMs,
                long tickIntervalMs,
                long tickAmount,
                int modifierBps,
                boolean negative,
                long generation,
                String sourceActorId,
                String abilityId,
                BattleAbilityKind abilityKind,
                String effectKey
        ) {
            this.id = id;
            this.expiresAtMs = expiresAtMs;
            this.tickIntervalMs = tickIntervalMs;
            this.tickAmount = tickAmount;
            this.modifierBps = modifierBps;
            this.negative = negative;
            this.generation = generation;
            this.sourceActorId = sourceActorId;
            this.abilityId = abilityId;
            this.abilityKind = abilityKind;
            this.effectKey = effectKey;
        }

        private static StatusState stronger(StatusState oldValue, StatusState newValue) {
            long tickAmount = Math.max(oldValue.tickAmount, newValue.tickAmount);
            int modifierBps = Math.max(oldValue.modifierBps, newValue.modifierBps);
            return new StatusState(
                    oldValue.id,
                    Math.max(oldValue.expiresAtMs, newValue.expiresAtMs),
                    Math.min(oldValue.tickIntervalMs, newValue.tickIntervalMs),
                    tickAmount,
                    modifierBps,
                    oldValue.negative || newValue.negative,
                    newValue.generation,
                    tickAmount == newValue.tickAmount || modifierBps == newValue.modifierBps ? newValue.sourceActorId : oldValue.sourceActorId,
                    tickAmount == newValue.tickAmount || modifierBps == newValue.modifierBps ? newValue.abilityId : oldValue.abilityId,
                    tickAmount == newValue.tickAmount || modifierBps == newValue.modifierBps ? newValue.abilityKind : oldValue.abilityKind,
                    tickAmount == newValue.tickAmount || modifierBps == newValue.modifierBps ? newValue.effectKey : oldValue.effectKey
            );
        }
    }

    private record DamageResult(long hpDamage, long shieldAbsorbed) {}
}
