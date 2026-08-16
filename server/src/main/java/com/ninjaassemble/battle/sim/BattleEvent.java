package com.ninjaassemble.battle.sim;

public record BattleEvent(long sequence, BattleEventType type, int round, String actorId, String targetId, long amount, boolean critical) {}
