package com.ninjaassemble.battle.sim;

public enum BattleAbilityKind {
    BASIC,
    SKILL1,
    SKILL2,
    RAGE_SKILL,
    AWAKENING_SKILL,
    PASSIVE,
    CONDITIONAL_SKILL,
    /** @deprecated compatibility name for pre-M49 data only. */
    @Deprecated ULTIMATE
}
