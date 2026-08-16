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
        int energyAfter,
        String effectType,
        String statusId,
        int durationTurns
) {
    public BattleEvent(long sequence, BattleEventType type, int round, String actorId, String targetId, long amount, boolean critical) {
        this(sequence, type, round, actorId, targetId, amount, critical, null, null, null, -1, null, null, 0);
    }

    public BattleEvent(long sequence, BattleEventType type, int round, String actorId, String targetId, long amount, boolean critical,
                       String abilityId, BattleAbilityKind abilityKind, String effectKey, int energyAfter) {
        this(sequence, type, round, actorId, targetId, amount, critical, abilityId, abilityKind, effectKey, energyAfter, null, null, 0);
    }
}
