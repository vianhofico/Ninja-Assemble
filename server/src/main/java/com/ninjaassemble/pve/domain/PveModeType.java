package com.ninjaassemble.pve.domain;

public enum PveModeType {
    // M62 release Resource PvE census.
    NARUTO_TRIAL,
    FOREST_HUNT,
    NINJA_TEST,
    GOLD_CHALLENGE,
    DAILY_FOOD,
    RESOURCE_RAID,
    BATTLE_RELIEF,
    OBITO_ULTIMATE_TRIAL,
    TAILED_BEAST_CONQUER,

    // Pre-M62 compatibility-only domain values. They are not release catalog entries.
    @Deprecated NINJA_TRIAL_AUTHENTIC_WATERFALL,
    @Deprecated NINJA_TRIAL_GAMA_TEMPLE,
    @Deprecated NINJA_TRIAL_PATH_OF_KUNOICHI,
    @Deprecated LAND_OF_PAIN,
    @Deprecated NINJA_QUEST,
    @Deprecated CHALLENGE,
    @Deprecated CRUSADE
}
