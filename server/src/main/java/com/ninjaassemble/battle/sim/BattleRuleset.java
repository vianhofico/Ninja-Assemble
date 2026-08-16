package com.ninjaassemble.battle.sim;

import com.ninjaassemble.reference.ReferenceProfiles;

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
        return new BattleRuleset(ReferenceProfiles.DAMAGE_FORMULA, 10_000, 1_000, 15_000, 50);
    }
}
