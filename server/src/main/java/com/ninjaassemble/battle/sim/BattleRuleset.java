package com.ninjaassemble.battle.sim;

/** Deterministic real-time battle rules. Values remain EXPERIMENTAL until reference evidence is collected. */
public record BattleRuleset(
        String version,
        int critChanceBps,
        long flatDefenseScale,
        int varianceBps,
        long maxBattleDurationMs,
        int referenceSpeed,
        long baseAttackIntervalMs,
        long minAttackIntervalMs,
        long maxAttackIntervalMs,
        int defaultBasicRageGain,
        int rageSkillCost,
        long aiDecisionIntervalMs
) {
    public BattleRuleset {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version required");
        if (critChanceBps < 0 || critChanceBps > 10_000 || varianceBps < 0 || varianceBps > 10_000) throw new IllegalArgumentException("invalid bps");
        if (flatDefenseScale <= 0 || maxBattleDurationMs <= 0 || referenceSpeed <= 0 || baseAttackIntervalMs <= 0) throw new IllegalArgumentException("positive timing/scaling required");
        if (minAttackIntervalMs <= 0 || maxAttackIntervalMs < minAttackIntervalMs) throw new IllegalArgumentException("invalid attack interval bounds");
        if (defaultBasicRageGain < 0 || defaultBasicRageGain > 100 || rageSkillCost < 1 || rageSkillCost > 100) throw new IllegalArgumentException("invalid Rage rules");
        if (aiDecisionIntervalMs <= 0) throw new IllegalArgumentException("aiDecisionIntervalMs required");
    }

    public static BattleRuleset experimentalV1() {
        return new BattleRuleset(
                "experimental-realtime-v1-unverified-formula",
                1_000,
                1_000,
                500,
                120_000L,
                1_000,
                1_500L,
                450L,
                3_000L,
                15,
                100,
                50L);
    }

    /** Experimental linear inverse-frequency model, clamped to safe bounds. */
    public long attackIntervalMs(int effectiveSpeed) {
        int speed = Math.max(1, effectiveSpeed);
        long raw = Math.max(1L, Math.round((double) baseAttackIntervalMs * referenceSpeed / speed));
        return Math.max(minAttackIntervalMs, Math.min(maxAttackIntervalMs, raw));
    }
}
