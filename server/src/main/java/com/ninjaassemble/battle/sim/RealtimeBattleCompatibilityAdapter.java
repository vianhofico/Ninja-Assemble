package com.ninjaassemble.battle.sim;

import java.util.List;

/**
 * Projects authoritative timestamped combat into the legacy battle response shape for old Unity clients.
 *
 * <p>The projection is presentation compatibility only. Gameplay outcome, rewards and progression must use the
 * {@link RealtimeBattleResult}. No second legacy simulation is executed.</p>
 */
public final class RealtimeBattleCompatibilityAdapter {
    private RealtimeBattleCompatibilityAdapter() {}

    public static BattleResult project(RealtimeBattleResult realtime, RealtimeBattleRuleset rules) {
        if (realtime == null || rules == null) throw new IllegalArgumentException("realtime result and rules required");
        long legacyTurnMs = rules.legacyTurnDurationMs();
        List<BattleEvent> events = realtime.events().stream()
                .map(event -> new BattleEvent(
                        event.sequence(),
                        event.type(),
                        projectedRound(event.type(), event.timestampMs(), legacyTurnMs),
                        event.actorId(),
                        event.targetId(),
                        event.amount(),
                        event.critical(),
                        event.abilityId(),
                        event.abilityKind(),
                        event.effectKey(),
                        event.energyAfter(),
                        event.effectType(),
                        event.statusId(),
                        projectedTurns(event.durationMs(), legacyTurnMs),
                        event.triggerId()
                ))
                .toList();
        return new BattleResult(
                realtime.seed(),
                realtime.rulesetVersion() + ":legacy-projection",
                realtime.outcome(),
                projectedTurns(realtime.durationMs(), legacyTurnMs),
                events,
                realtime.finalHp()
        );
    }

    private static int projectedRound(BattleEventType type, long timestampMs, long legacyTurnMs) {
        if (type == BattleEventType.BATTLE_START || timestampMs <= 0) return 0;
        return Math.max(1, projectedTurns(timestampMs, legacyTurnMs));
    }

    private static int projectedTurns(long durationMs, long legacyTurnMs) {
        if (durationMs <= 0) return 0;
        long turns = Math.addExact(durationMs, legacyTurnMs - 1) / legacyTurnMs;
        return Math.toIntExact(turns);
    }
}
