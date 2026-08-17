package com.ninjaassemble.battle.sim;

/** Timestamped replay event consumed by Unity presentation. */
public record RealtimeBattleEvent(
        long sequence,
        long timestampMs,
        BattleEventType type,
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
        long durationMs,
        String triggerId
) {
    public RealtimeBattleEvent {
        if (sequence < 0 || timestampMs < 0 || type == null) throw new IllegalArgumentException("invalid realtime event identity");
        if (durationMs < 0) throw new IllegalArgumentException("event duration cannot be negative");
    }
}
