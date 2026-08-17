package com.ninjaassemble.battle.sim;

/** Event/time-based passive hooks. No trigger is defined in terms of a combat turn or round. */
public enum PassiveTrigger {
    BATTLE_START,
    TIME_INTERVAL,
    BEFORE_ACTION,
    AFTER_ACTION,
    BEFORE_DAMAGE,
    AFTER_DAMAGE_DEALT,
    AFTER_DAMAGE_TAKEN,
    HP_THRESHOLD,
    ALLY_KO,
    ENEMY_KO,
    STATUS_APPLIED,
    SKILL_CAST,
    RAGE_SKILL_CAST,
    DODGE,
    CRITICAL
}
