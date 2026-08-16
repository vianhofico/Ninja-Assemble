package com.ninjaassemble.reference;

import java.util.Map;

public final class ReferenceProfiles {
    public static final String COMBAT_STATS = "experimental-combat-stats-v1";
    public static final String DAMAGE_FORMULA = "experimental-v1-unverified-formula";
    public static final String COMPLETE_ROSTER_SUMMON = "complete-roster-experimental-v1";
    public static final String HERO_LEVEL_COST = "experimental-level-cost-v1";
    public static final String ABILITY_CYCLE = "experimental-ability-cycle-v1";

    private static final Map<String, ReferenceConfidence> CONFIDENCE = Map.of(
            COMBAT_STATS, ReferenceConfidence.EXPERIMENTAL,
            DAMAGE_FORMULA, ReferenceConfidence.EXPERIMENTAL,
            COMPLETE_ROSTER_SUMMON, ReferenceConfidence.EXPERIMENTAL,
            HERO_LEVEL_COST, ReferenceConfidence.EXPERIMENTAL,
            ABILITY_CYCLE, ReferenceConfidence.EXPERIMENTAL
    );

    private ReferenceProfiles() {}

    public static ReferenceConfidence confidence(String profileId) {
        ReferenceConfidence value = CONFIDENCE.get(profileId);
        if (value == null) throw new IllegalArgumentException("unknown reference profile: " + profileId);
        return value;
    }
}
