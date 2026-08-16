package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;

public final class DeterministicBattleEngine {
    private static final Set<String> DOT_STATUSES = Set.of("BURN", "POISON", "BLEED");
    private static final Set<String> CONTROL_STATUSES = Set.of("STUN", "SILENCE");

    public BattleResult simulate(BattleRequest request) {
        SplittableRandom random = new SplittableRandom(request.seed());
        List<State> states = request.units().stream().map(State::new).toList();
        EventSink sink = new EventSink();
        sink.emit(BattleEventType.BATTLE_START, 0, null, null, 0, false, null, null, null, -1, null, null, 0);

        states.stream().sorted(Comparator.comparing(State::side).thenComparingInt(State::slot))
                .forEach(owner -> triggerPassives(owner, PassiveTrigger.BATTLE_START, states, request.ruleset(), random, 0, sink));

        int completedRounds = 0;
        for (int round = 1; round <= request.ruleset().maxRounds(); round++) {
            if (winner(states) != null) break;
            completedRounds = round;
            sink.emit(BattleEventType.ROUND_START, round, null, null, 0, false, null, null, null, -1, null, null, 0);
            List<State> order = states.stream().filter(State::alive)
                    .sorted(Comparator.comparingInt(State::speed).reversed().thenComparing(State::side).thenComparingInt(State::slot))
                    .toList();

            for (State actor : order) {
                if (!actor.alive() || winner(states) != null) continue;
                processTurnStart(actor, states, request.ruleset(), random, round, sink);
                if (!actor.alive()) {
                    actor.advanceStatuses();
                    continue;
                }

                triggerPassives(actor, PassiveTrigger.TURN_START, states, request.ruleset(), random, round, sink);
                triggerPassives(actor, PassiveTrigger.SELF_LOW_HP, states, request.ruleset(), random, round, sink);

                if (actor.hasStatus("STUN")) {
                    sink.emit(BattleEventType.TURN_SKIPPED, round, actor.id(), actor.id(), 0, false,
                            null, null, null, actor.energy, "STATUS", "STUN", actor.statusDuration("STUN"));
                    actor.advanceStatuses();
                    continue;
                }

                boolean silenced = actor.hasStatus("SILENCE");
                BattleAbility ability = actor.nextAbility(silenced);
                int energyAfter = actor.applyEnergy(ability.energyDelta());
                List<SkillEffectDefinition> effects = ability.effects().isEmpty() ? List.of(fallbackDamage(ability)) : ability.effects();
                State primaryTarget = firstTarget(effects, states, actor, random, false);
                sink.emit(BattleEventType.ATTACK, round, actor.id(), primaryTarget == null ? null : primaryTarget.id(), 0, false,
                        ability.id(), ability.kind(), ability.effectKey(), energyAfter, null, null, 0);

                for (SkillEffectDefinition effect : effects) {
                    applyEffect(effect, ability, actor, states, request.ruleset(), random, round, sink, false);
                    if (winner(states) != null) break;
                }
                actor.advanceStatuses();
            }
        }

        BattleOutcome outcome = outcome(states);
        sink.emit(BattleEventType.BATTLE_END, completedRounds, null, null, 0, false, null, null, null, -1, null, null, 0);
        Map<String, Long> hp = new LinkedHashMap<>();
        states.stream().sorted(Comparator.comparing(State::side).thenComparingInt(State::slot)).forEach(it -> hp.put(it.id(), it.hp));
        return new BattleResult(request.seed(), request.ruleset().version(), outcome, completedRounds, List.copyOf(sink.events), Map.copyOf(hp));
    }

    private static SkillEffectDefinition fallbackDamage(BattleAbility ability) {
        return new SkillEffectDefinition(EffectType.DAMAGE, TargetSelector.FRONTMOST_ENEMY, ability.channel(),
                ability.coefficientBps(), 0, null, 10_000, 0);
    }

    private static void processTurnStart(State actor, List<State> states, BattleRuleset rules, SplittableRandom random,
                                         int round, EventSink sink) {
        for (StatusState status : List.copyOf(actor.statuses.values())) {
            if (!DOT_STATUSES.contains(status.id) || status.tickAmount <= 0 || !actor.alive()) continue;
            DamageResult damage = actor.damage(status.tickAmount);
            if (damage.shieldAbsorbed > 0) {
                sink.emit(BattleEventType.SHIELD_ABSORB, round, actor.id(), actor.id(), damage.shieldAbsorbed, false,
                        null, null, null, actor.energy, "STATUS", status.id, status.remainingTurns);
            }
            sink.emit(BattleEventType.STATUS_TICK, round, actor.id(), actor.id(), damage.hpDamage, false,
                    null, null, null, actor.energy, "STATUS", status.id, status.remainingTurns);
            if (actor.alive()) {
                triggerPassives(actor, PassiveTrigger.AFTER_DAMAGE_TAKEN, states, rules, random, round, sink);
                triggerPassives(actor, PassiveTrigger.SELF_LOW_HP, states, rules, random, round, sink);
            } else {
                sink.emit(BattleEventType.KO, round, actor.id(), actor.id(), 0, false,
                        null, null, null, actor.energy, "STATUS", status.id, status.remainingTurns);
                triggerAllyKo(actor, states, rules, random, round, sink);
            }
        }
    }

    private static void triggerPassives(State owner, PassiveTrigger trigger, List<State> states, BattleRuleset rules,
                                        SplittableRandom random, int round, EventSink sink) {
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
                    "vfx/passives/" + passive.id(), passive.effects());
            sink.emitPassive(round, owner.id(), passive.id(), trigger, owner.energy);
            for (SkillEffectDefinition effect : passive.effects()) {
                applyEffect(effect, wrapper, owner, states, rules, random, round, sink, true);
            }
        }
    }

    private static void triggerAllyKo(State defeated, List<State> states, BattleRuleset rules, SplittableRandom random,
                                      int round, EventSink sink) {
        states.stream().filter(State::alive).filter(it -> it.side() == defeated.side()).filter(it -> it != defeated)
                .sorted(Comparator.comparingInt(State::slot).thenComparing(State::id))
                .forEach(owner -> triggerPassives(owner, PassiveTrigger.ALLY_KO, states, rules, random, round, sink));
    }

    private static State firstTarget(List<SkillEffectDefinition> effects, List<State> states, State actor,
                                     SplittableRandom random, boolean consumeRandom) {
        for (SkillEffectDefinition effect : effects) {
            List<State> resolved = targets(effect, states, actor, random, consumeRandom);
            if (!resolved.isEmpty()) return resolved.get(0);
        }
        return null;
    }

    private static void applyEffect(SkillEffectDefinition effect, BattleAbility ability, State actor, List<State> states,
                                    BattleRuleset rules, SplittableRandom random, int round, EventSink sink,
                                    boolean passiveContext) {
        List<State> resolved = targets(effect, states, actor, random, true);
        for (State target : resolved) {
            switch (effect.type()) {
                case DAMAGE -> applyDamage(effect, ability, actor, target, states, rules, random, round, sink, passiveContext);
                case HEAL -> applyHeal(effect, ability, actor, target, round, sink);
                case SHIELD -> applyShield(effect, ability, actor, target, round, sink);
                case ENERGY -> applyEnergy(effect, ability, actor, target, round, sink);
                case STATUS -> applyStatus(effect, ability, actor, target, random, round, sink);
                case CLEANSE -> applyCleanse(ability, actor, target, round, sink, true);
                case DISPEL -> applyCleanse(ability, actor, target, round, sink, false);
                case REVIVE -> applyRevive(effect, ability, actor, target, round, sink);
            }
        }
    }

    private static void applyDamage(SkillEffectDefinition effect, BattleAbility ability, State actor, State target,
                                    List<State> states, BattleRuleset rules, SplittableRandom random, int round,
                                    EventSink sink, boolean passiveContext) {
        DamageChannel channel = effect.channel() == null ? ability.channel() : effect.channel();
        int coefficient = effect.coefficientBps() > 0 ? effect.coefficientBps() : ability.coefficientBps();
        boolean critical = rollCritical(random, actor.seed, channel);
        long damage = damage(actor, target, channel, coefficient, effect.flatAmount(), rules, critical);
        DamageResult applied = target.damage(damage);
        if (applied.shieldAbsorbed > 0) {
            sink.emit(BattleEventType.SHIELD_ABSORB, round, actor.id(), target.id(), applied.shieldAbsorbed, critical,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0);
        }
        sink.emit(BattleEventType.DAMAGE, round, actor.id(), target.id(), applied.hpDamage, critical,
                ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0);
        if (!target.alive()) {
            sink.emit(BattleEventType.KO, round, actor.id(), target.id(), 0, false,
                    ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0);
        }

        if (!passiveContext && applied.hpDamage > 0) {
            if (actor.alive()) triggerPassives(actor, PassiveTrigger.AFTER_DAMAGE_DEALT, states, rules, random, round, sink);
            if (target.alive()) {
                triggerPassives(target, PassiveTrigger.AFTER_DAMAGE_TAKEN, states, rules, random, round, sink);
                triggerPassives(target, PassiveTrigger.SELF_LOW_HP, states, rules, random, round, sink);
            } else {
                triggerAllyKo(target, states, rules, random, round, sink);
            }
        }
    }

    private static void applyHeal(SkillEffectDefinition effect, BattleAbility ability, State actor, State target, int round, EventSink sink) {
        if (!target.alive()) return;
        long requested = scaledAmount(actor, effect.channel() == null ? ability.channel() : effect.channel(), effect.coefficientBps(), effect.flatAmount());
        long applied = target.heal(requested);
        sink.emit(BattleEventType.HEAL, round, actor.id(), target.id(), applied, false,
                ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0);
    }

    private static void applyShield(SkillEffectDefinition effect, BattleAbility ability, State actor, State target, int round, EventSink sink) {
        if (!target.alive()) return;
        long amount = scaledAmount(actor, effect.channel() == null ? ability.channel() : effect.channel(), effect.coefficientBps(), effect.flatAmount());
        long applied = target.addShield(amount);
        sink.emit(BattleEventType.SHIELD, round, actor.id(), target.id(), applied, false,
                ability.id(), ability.kind(), ability.effectKey(), actor.energy, effect.type().name(), null, 0);
    }

    private static void applyEnergy(SkillEffectDefinition effect, BattleAbility ability, State actor, State target, int round, EventSink sink) {
        int requested = Math.toIntExact(Math.max(-100, Math.min(100, effect.flatAmount())));
        int before = target.energy;
        int after = target.applyEnergy(requested);
        sink.emit(BattleEventType.ENERGY, round, actor.id(), target.id(), after - before, false,
                ability.id(), ability.kind(), ability.effectKey(), after, effect.type().name(), null, 0);
    }

    private static void applyStatus(SkillEffectDefinition effect, BattleAbility ability, State actor, State target,
                                    SplittableRandom random, int round, EventSink sink) {
        if (!target.alive() || effect.status() == null || effect.status().isBlank()) return;
        int chance = effect.chanceBps() == 0 ? 10_000 : effect.chanceBps();
        if (random.nextInt(10_000) >= chance) return;
        String statusId = normalizeStatus(effect.status());
        int duration = Math.max(1, effect.durationTurns());
        long tickAmount = DOT_STATUSES.contains(statusId)
                ? scaledAmount(actor, effect.channel() == null ? ability.channel() : effect.channel(), effect.coefficientBps(), effect.flatAmount())
                : 0;
        int modifierBps = DOT_STATUSES.contains(statusId) || CONTROL_STATUSES.contains(statusId) ? 0 : effect.coefficientBps();
        target.applyStatus(new StatusState(statusId, duration, tickAmount, modifierBps, isNegativeStatus(statusId)));
        sink.emit(BattleEventType.STATUS_APPLIED, round, actor.id(), target.id(), 0, false,
                ability.id(), ability.kind(), ability.effectKey(), target.energy, effect.type().name(), statusId, duration);
    }

    private static void applyCleanse(BattleAbility ability, State actor, State target, int round, EventSink sink, boolean negative) {
        for (String removed : target.removeStatuses(negative)) {
            sink.emit(BattleEventType.STATUS_CLEANSED, round, actor.id(), target.id(), 0, false,
                    ability.id(), ability.kind(), ability.effectKey(), target.energy,
                    negative ? EffectType.CLEANSE.name() : EffectType.DISPEL.name(), removed, 0);
        }
    }

    private static void applyRevive(SkillEffectDefinition effect, BattleAbility ability, State actor, State target, int round, EventSink sink) {
        if (target.alive()) return;
        long amount = effect.flatAmount() > 0 ? effect.flatAmount()
                : Math.max(1, target.seed.maxHp() * Math.max(1, effect.coefficientBps()) / 10_000);
        long restored = target.revive(amount);
        sink.emit(BattleEventType.REVIVE, round, actor.id(), target.id(), restored, false,
                ability.id(), ability.kind(), ability.effectKey(), target.energy, effect.type().name(), null, 0);
    }

    private static List<State> targets(SkillEffectDefinition effect, List<State> states, State actor,
                                       SplittableRandom random, boolean consumeRandom) {
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

    private static List<State> first(List<State> values) { return values.isEmpty() ? List.of() : List.of(values.get(0)); }

    private static List<State> randomOne(List<State> values, SplittableRandom random) {
        return values.isEmpty() ? List.of() : List.of(values.get(random.nextInt(values.size())));
    }

    private static boolean rollCritical(SplittableRandom random, BattleUnitSeed actor, DamageChannel channel) {
        int chance = channel == DamageChannel.PHYSICAL ? actor.physicalCritBps() : actor.chakraCritBps();
        return random.nextInt(10_000) < chance;
    }

    private static long damage(State actor, State target, DamageChannel channel, int coefficientBps, long flatAmount,
                               BattleRuleset rules, boolean critical) {
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

    private static String normalizeStatus(String value) { return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'); }

    private static boolean isNegativeStatus(String status) {
        return CONTROL_STATUSES.contains(status) || DOT_STATUSES.contains(status) || status.endsWith("_DOWN") || status.startsWith("DEBUFF_");
    }

    private static TeamSide winner(List<State> states) {
        boolean a = states.stream().anyMatch(it -> it.alive() && it.side() == TeamSide.A);
        boolean b = states.stream().anyMatch(it -> it.alive() && it.side() == TeamSide.B);
        if (a == b) return null;
        return a ? TeamSide.A : TeamSide.B;
    }

    private static BattleOutcome outcome(List<State> states) {
        TeamSide winner = winner(states);
        return winner == TeamSide.A ? BattleOutcome.TEAM_A : winner == TeamSide.B ? BattleOutcome.TEAM_B : BattleOutcome.DRAW;
    }

    private static final class EventSink {
        private final List<BattleEvent> events = new ArrayList<>();
        private long sequence;

        private void emit(BattleEventType type, int round, String actorId, String targetId, long amount, boolean critical,
                          String abilityId, BattleAbilityKind abilityKind, String effectKey, int energyAfter,
                          String effectType, String statusId, int durationTurns) {
            events.add(new BattleEvent(sequence++, type, round, actorId, targetId, amount, critical,
                    abilityId, abilityKind, effectKey, energyAfter, effectType, statusId, durationTurns));
        }

        private void emitPassive(int round, String ownerId, String passiveId, PassiveTrigger trigger, int energyAfter) {
            events.add(new BattleEvent(sequence++, BattleEventType.PASSIVE_TRIGGER, round, ownerId, ownerId, 0, false,
                    passiveId, BattleAbilityKind.PASSIVE, "vfx/passives/" + passiveId, energyAfter,
                    "PASSIVE", null, 0, trigger.name()));
        }
    }

    private static final class State {
        private final BattleUnitSeed seed;
        private final Map<String, StatusState> statuses = new LinkedHashMap<>();
        private final Set<String> firedPassives = new HashSet<>();
        private long hp;
        private long shield;
        private int energy;
        private int comboStep;

        private State(BattleUnitSeed seed) { this.seed = seed; this.hp = seed.maxHp(); }
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

        private BattleAbility nextAbility(boolean silenced) {
            if (silenced) return seed.abilities().basic();
            if (energy >= 100) { comboStep = 0; return seed.abilities().ultimate(); }
            BattleAbility ability = switch (comboStep) {
                case 0 -> seed.abilities().basic();
                case 1 -> seed.abilities().skill1();
                default -> seed.abilities().skill2();
            };
            comboStep = (comboStep + 1) % 3;
            return ability;
        }

        private int applyEnergy(int delta) { energy = Math.max(0, Math.min(100, energy + delta)); return energy; }
        private boolean hasStatus(String id) { return statuses.containsKey(id); }
        private int statusDuration(String id) { StatusState value = statuses.get(id); return value == null ? 0 : value.remainingTurns; }
        private void applyStatus(StatusState status) { statuses.merge(status.id, status, StatusState::stronger); }

        private void advanceStatuses() {
            List<String> expired = new ArrayList<>();
            for (Map.Entry<String, StatusState> entry : statuses.entrySet()) {
                StatusState status = entry.getValue();
                status.remainingTurns--;
                if (status.remainingTurns <= 0) expired.add(entry.getKey());
            }
            expired.forEach(statuses::remove);
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

        private long heal(long value) { long applied = Math.min(seed.maxHp() - hp, Math.max(0, value)); hp += applied; return applied; }
        private long addShield(long value) { long applied = Math.max(0, value); shield = Math.addExact(shield, applied); return applied; }
        private long revive(long value) { if (alive()) return 0; hp = Math.min(seed.maxHp(), Math.max(1, value)); statuses.clear(); shield = 0; return hp; }
    }

    private static final class StatusState {
        private final String id;
        private int remainingTurns;
        private final long tickAmount;
        private final int modifierBps;
        private final boolean negative;

        private StatusState(String id, int remainingTurns, long tickAmount, int modifierBps, boolean negative) {
            this.id = id; this.remainingTurns = remainingTurns; this.tickAmount = tickAmount; this.modifierBps = modifierBps; this.negative = negative;
        }

        private static StatusState stronger(StatusState oldValue, StatusState newValue) {
            return new StatusState(oldValue.id, Math.max(oldValue.remainingTurns, newValue.remainingTurns),
                    Math.max(oldValue.tickAmount, newValue.tickAmount), Math.max(oldValue.modifierBps, newValue.modifierBps),
                    oldValue.negative || newValue.negative);
        }
    }

    private record DamageResult(long hpDamage, long shieldAbsorbed) {}
}
