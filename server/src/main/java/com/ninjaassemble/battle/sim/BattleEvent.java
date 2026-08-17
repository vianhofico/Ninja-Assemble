package com.ninjaassemble.battle.sim;

/** Immutable presentation/audit event emitted from authoritative logical simulation time. */
public record BattleEvent(
        int sequence,
        long timestampMs,
        BattleEventType type,
        String actorId,
        String targetId,
        long amount,
        boolean critical,
        String abilityId,
        BattleAbilityKind abilityKind,
        String effectKey,
        int rageAfter,
        String effectType,
        String statusId,
        long durationMs,
        String triggerId
) {
    public BattleEvent {
        if (sequence < 0 || timestampMs < 0 || type == null) throw new IllegalArgumentException("invalid event identity/time");
        if (rageAfter < 0 || rageAfter > 100) throw new IllegalArgumentException("rageAfter outside 0..100");
        if (durationMs < 0) throw new IllegalArgumentException("durationMs cannot be negative");
    }

    public static BattleEvent simple(int sequence, long timestampMs, BattleEventType type, String actorId, String targetId) {
        return new BattleEvent(sequence, timestampMs, type, actorId, targetId, 0, false, null, null, null, 0, null, null, 0, null);
    }

    /** @deprecated Temporary DTO compatibility while Unity is migrated in this same milestone. */
    @Deprecated(forRemoval = true)
    public int energyAfter() { return rageAfter; }
}
