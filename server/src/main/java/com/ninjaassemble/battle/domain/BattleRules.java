package com.ninjaassemble.battle.domain;

public final class BattleRules {
    public static final int ARENA_TEAM_SIZE = 5;
    public static final int SHADOW_SQUAD_SIZE = 5;
    public static final int SHADOW_SQUAD_COUNT = 3;
    public static final int SHADOW_ROSTER_SIZE = SHADOW_SQUAD_SIZE * SHADOW_SQUAD_COUNT;
    public static final int SHADOW_WINS_REQUIRED = 2;

    private BattleRules() {}

    public static void requireArenaTeamSize(int size) { requireExact("Arena team", size, ARENA_TEAM_SIZE); }
    public static void requireShadowRosterSize(int size) { requireExact("Shadow Arena roster", size, SHADOW_ROSTER_SIZE); }
    public static void requireShadowSquadSize(int size) { requireExact("Shadow Arena squad", size, SHADOW_SQUAD_SIZE); }

    private static void requireExact(String label, int actual, int expected) {
        if (actual != expected) throw new IllegalArgumentException(label + " must contain exactly " + expected + " ninjas, got " + actual);
    }
}
