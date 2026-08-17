package com.ninjaassemble.reference;

import java.util.Map;

public final class ReferenceProfiles {
    public static final String COMBAT_STATS = "experimental-combat-stats-v1";
    public static final String DAMAGE_FORMULA = "experimental-v1-unverified-formula";
    public static final String COMPLETE_ROSTER_SUMMON = "complete-roster-hero-version-experimental-v2";
    public static final String HERO_LEVEL_COST = "experimental-level-cost-v1";
    public static final String ABILITY_CYCLE = "experimental-ability-cycle-v1";
    public static final String STRUCTURED_EFFECTS = "experimental-structured-effects-v1";
    public static final String TECHNIQUE_MAPPING = "experimental-technique-mapping-v1";
    public static final String PASSIVE_LIFECYCLE = "experimental-passive-lifecycle-v1";
    public static final String REALTIME_TIMING = "experimental-realtime-timing-v1";
    public static final String RAGE_RULES = "experimental-rage-v1";

    private static final Map<String, ReferenceConfidence> CONFIDENCE = Map.ofEntries(
            Map.entry(COMBAT_STATS, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(DAMAGE_FORMULA, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(COMPLETE_ROSTER_SUMMON, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(HERO_LEVEL_COST, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(ABILITY_CYCLE, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(STRUCTURED_EFFECTS, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(TECHNIQUE_MAPPING, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(PASSIVE_LIFECYCLE, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(REALTIME_TIMING, ReferenceConfidence.EXPERIMENTAL),
            Map.entry(RAGE_RULES, ReferenceConfidence.EXPERIMENTAL)
    );

    private ReferenceProfiles() {}

    public static ReferenceConfidence confidence(String profileId) {
        ReferenceConfidence value = CONFIDENCE.get(profileId);
        if (value == null) throw new IllegalArgumentException("unknown reference profile: " + profileId);
        return value;
    }
}
