package com.ninjaassemble.battle.sim;

public record BattleRuleset(
        String version,
        int basicAttackCoefficientBps,
        long defenseScale,
        int criticalMultiplierBps,
        int maxRounds
) {
    public BattleRuleset {
        if (version == null || version.isBlank()) throw new IllegalArgumentException("ruleset version required");
        if (basicAttackCoefficientBps <= 0 || defenseScale <= 0 || criticalMultiplierBps < 10_000 || maxRounds <= 0) throw new IllegalArgumentException("invalid ruleset values");
    }

    public static BattleRuleset experimentalV1() {
        return new BattleRuleset("experimental-v1-unverified-formula", 10_000, 1_000, 15_000, 50);
    }
}
