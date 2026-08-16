package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public final class DeterministicBattleEngine {
    public BattleResult simulate(BattleRequest request) {
        SplittableRandom random = new SplittableRandom(request.seed());
        List<State> states = request.units().stream().map(State::new).toList();
        List<BattleEvent> events = new ArrayList<>();
        long seq = 0;
        events.add(new BattleEvent(seq++, BattleEventType.BATTLE_START, 0, null, null, 0, false));

        int completedRounds = 0;
        for (int round = 1; round <= request.ruleset().maxRounds(); round++) {
            if (winner(states) != null) break;
            completedRounds = round;
            events.add(new BattleEvent(seq++, BattleEventType.ROUND_START, round, null, null, 0, false));
            List<State> order = states.stream().filter(State::alive)
                    .sorted(Comparator.comparingInt(State::speed).reversed().thenComparing(State::side).thenComparingInt(State::slot))
                    .toList();
            for (State actor : order) {
                if (!actor.alive() || winner(states) != null) break;
                State target = selectFrontmostEnemy(states, actor.side());
                if (target == null) break;

                BattleAbility ability = actor.nextAbility();
                boolean critical = rollCritical(random, actor.seed, ability.channel());
                long damage = damage(actor.seed, target.seed, ability, request.ruleset(), critical);
                int energyAfter = actor.applyEnergy(ability.energyDelta());

                events.add(new BattleEvent(seq++, BattleEventType.ATTACK, round, actor.id(), target.id(), 0, critical,
                        ability.id(), ability.kind(), ability.effectKey(), energyAfter));
                long applied = target.damage(damage);
                events.add(new BattleEvent(seq++, BattleEventType.DAMAGE, round, actor.id(), target.id(), applied, critical,
                        ability.id(), ability.kind(), ability.effectKey(), energyAfter));
                if (!target.alive()) {
                    events.add(new BattleEvent(seq++, BattleEventType.KO, round, actor.id(), target.id(), 0, false,
                            ability.id(), ability.kind(), ability.effectKey(), energyAfter));
                }
            }
        }

        BattleOutcome outcome = outcome(states);
        events.add(new BattleEvent(seq, BattleEventType.BATTLE_END, completedRounds, null, null, 0, false));
        Map<String, Long> hp = new LinkedHashMap<>();
        states.stream().sorted(Comparator.comparing(State::side).thenComparingInt(State::slot)).forEach(it -> hp.put(it.id(), it.hp));
        return new BattleResult(request.seed(), request.ruleset().version(), outcome, completedRounds, List.copyOf(events), Map.copyOf(hp));
    }

    private static State selectFrontmostEnemy(List<State> states, TeamSide side) {
        return states.stream().filter(State::alive).filter(it -> it.side() != side)
                .min(Comparator.comparingInt(State::slot).thenComparing(State::id)).orElse(null);
    }

    private static boolean rollCritical(SplittableRandom random, BattleUnitSeed actor, DamageChannel channel) {
        int chance = channel == DamageChannel.PHYSICAL ? actor.physicalCritBps() : actor.chakraCritBps();
        return random.nextInt(10_000) < chance;
    }

    private static long damage(BattleUnitSeed actor, BattleUnitSeed target, BattleAbility ability, BattleRuleset rules, boolean critical) {
        boolean physical = ability.channel() == DamageChannel.PHYSICAL;
        long attack = physical ? actor.physicalAttack() : actor.chakraAttack();
        long defense = physical ? target.physicalDefense() : target.chakraDefense();
        long raw = Math.max(1, Math.multiplyExact(attack, ability.coefficientBps()) / 10_000);
        long mitigated = Math.max(1, raw * rules.defenseScale() / (rules.defenseScale() + defense));
        return critical ? Math.max(1, mitigated * rules.criticalMultiplierBps() / 10_000) : mitigated;
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

    private static final class State {
        private final BattleUnitSeed seed;
        private long hp;
        private int energy;
        private int comboStep;

        private State(BattleUnitSeed seed) {
            this.seed = seed;
            this.hp = seed.maxHp();
        }

        private BattleAbility nextAbility() {
            if (energy >= 100) {
                comboStep = 0;
                return seed.abilities().ultimate();
            }
            BattleAbility ability = switch (comboStep) {
                case 0 -> seed.abilities().basic();
                case 1 -> seed.abilities().skill1();
                default -> seed.abilities().skill2();
            };
            comboStep = (comboStep + 1) % 3;
            return ability;
        }

        private int applyEnergy(int delta) {
            energy = Math.max(0, Math.min(100, energy + delta));
            return energy;
        }

        private boolean alive() { return hp > 0; }
        private String id() { return seed.id(); }
        private TeamSide side() { return seed.side(); }
        private int slot() { return seed.slot(); }
        private int speed() { return seed.speed(); }
        private long damage(long value) { long applied = Math.min(hp, Math.max(0, value)); hp -= applied; return applied; }
    }
}
