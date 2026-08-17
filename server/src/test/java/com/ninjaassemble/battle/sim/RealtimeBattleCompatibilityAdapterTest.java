package com.ninjaassemble.battle.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RealtimeBattleCompatibilityAdapterTest {
    @Test
    void timestampedReplayProjectsIntoLegacyShapeWithoutSecondSimulation() {
        RealtimeBattleRuleset rules = RealtimeBattleRuleset.experimentalV1();
        RealtimeBattleResult realtime = new RealtimeBattleResult(
                77L,
                rules.version(),
                BattleOutcome.TEAM_A,
                9_000L,
                List.of(
                        event(0, 0L, BattleEventType.BATTLE_START, 0L),
                        event(1, 4_500L, BattleEventType.DAMAGE, 4_500L),
                        event(2, 9_000L, BattleEventType.BATTLE_END, 0L)
                ),
                Map.of("a", 100L, "b", 0L)
        );

        BattleResult projected = RealtimeBattleCompatibilityAdapter.project(realtime, rules);

        assertEquals(77L, projected.seed());
        assertEquals(rules.version() + ":legacy-projection", projected.rulesetVersion());
        assertEquals(BattleOutcome.TEAM_A, projected.outcome());
        assertEquals(3, projected.rounds());
        assertEquals(0, projected.events().get(0).round());
        assertEquals(2, projected.events().get(1).round());
        assertEquals(2, projected.events().get(1).durationTurns());
        assertEquals(3, projected.events().get(2).round());
        assertEquals(realtime.finalHp(), projected.finalHp());
    }

    private static RealtimeBattleEvent event(long sequence, long timestampMs, BattleEventType type, long durationMs) {
        return new RealtimeBattleEvent(
                sequence,
                timestampMs,
                type,
                "a",
                "b",
                type == BattleEventType.DAMAGE ? 25L : 0L,
                false,
                "test-ability",
                BattleAbilityKind.BASIC,
                "vfx/techniques/test-ability",
                30,
                type == BattleEventType.DAMAGE ? "DAMAGE" : null,
                null,
                durationMs,
                null
        );
    }
}
