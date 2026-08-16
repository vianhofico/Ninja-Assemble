package com.ninjaassemble.battle.sim;

public record BattleEvent(
        long sequence,
        BattleEventType type,
        int round,
        String actorId,
        String targetId,
        long amount,
        boolean critical,
        String abilityId,
        BattleAbilityKind abilityKind,
        String effectKey,
        int energyAfter
) {
    public BattleEvent(long sequence, BattleEventType type, int round, String actorId, String targetId, long amount, boolean critical) {
        this(sequence, type, round, actorId, targetId, amount, critical, null, null, null, -1);
    }
}
