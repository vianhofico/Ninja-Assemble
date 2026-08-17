package com.ninjaassemble.battle.sim;

import com.ninjaassemble.reference.ReferenceProfiles;

/** Deterministic wall-clock rules for continuous-time auto combat. */
public record RealtimeBattleRuleset(
        String version,
        int simulationTickMs,
        int baseActionIntervalMs,
        int minActionIntervalMs,
        int maxActionIntervalMs,
        long defenseScale,
        int criticalMultiplierBps,
        long maxBattleDurationMs,
        long legacyTurnDurationMs,
        long defaultStatusTickIntervalMs
) {
    public RealtimeBattleRuleset {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("ruleset version required");
        if (simulationTickMs <= 0 || baseActionIntervalMs <= 0 || minActionIntervalMs <= 0 || maxActionIntervalMs < minActionIntervalMs) {
            throw new IllegalArgumentException("invalid action timing values");
        }
        if (defenseScale <= 0 || criticalMultiplierBps < 10_000 || maxBattleDurationMs <= 0 || legacyTurnDurationMs <= 0 || defaultStatusTickIntervalMs <= 0) {
            throw new IllegalArgumentException("invalid realtime ruleset values");
        }
    }

    public static RealtimeBattleRuleset experimentalV1() {
        return new RealtimeBattleRuleset(
                ReferenceProfiles.DAMAGE_FORMULA + ":realtime-v1",
                50,
                3_000,
                500,
                6_000,
                1_000,
                15_000,
                180_000,
                3_000,
                1_000
        );
    }

    /**
     * Convert speed into an independent action interval. A speed of 100 maps to baseActionIntervalMs.
     * The result is clamped and quantized to the fixed simulation tick.
     */
    public long actionIntervalMs(int speed) {
        long safeSpeed = Math.max(1, speed);
        long raw = Math.multiplyExact((long) baseActionIntervalMs, 100L) / safeSpeed;
        long clamped = Math.max(minActionIntervalMs, Math.min(maxActionIntervalMs, raw));
        return quantizeUp(clamped);
    }

    public long quantizeUp(long timestampMs) {
        if (timestampMs <= 0) return 0L;
        long tick = simulationTickMs;
        long remainder = timestampMs % tick;
        return remainder == 0 ? timestampMs : Math.addExact(timestampMs, tick - remainder);
    }
}
