package com.ninjaassemble.hero.domain;

public enum EffectType {
    DAMAGE,
    HEAL,
    RAGE,
    /** @deprecated legacy ultimate-resource name; runtime content must use RAGE. */
    @Deprecated ENERGY,
    STATUS,
    CLEANSE,
    DISPEL,
    REVIVE,
    SHIELD
}
