package com.ninjaassemble.hero.domain;

import com.ninjaassemble.battle.domain.DamageChannel;

/**
 * Data-driven skill effect definition.
 *
 * <p>Real-time combat uses millisecond durations and tick intervals. The legacy durationTurns field is kept
 * temporarily so existing content and the old deterministic round engine can compile during migration.</p>
 */
public record SkillEffectDefinition(
        EffectType type,
        TargetSelector target,
        DamageChannel channel,
        int coefficientBps,
        long flatAmount,
        String status,
        int chanceBps,
        int durationTurns,
        long durationMs,
        long tickIntervalMs
) {
    public SkillEffectDefinition {
        if (type == null || target == null) throw new IllegalArgumentException("effect type/target required");
        if (coefficientBps < 0 || chanceBps < 0 || chanceBps > 10_000 || durationTurns < 0 || durationMs < 0 || tickIntervalMs < 0) {
            throw new IllegalArgumentException("invalid effect values");
        }
    }

    /** Compatibility constructor for legacy turn-authored content. */
    public SkillEffectDefinition(
            EffectType type,
            TargetSelector target,
            DamageChannel channel,
            int coefficientBps,
            long flatAmount,
            String status,
            int chanceBps,
            int durationTurns
    ) {
        this(type, target, channel, coefficientBps, flatAmount, status, chanceBps, durationTurns, 0L, 0L);
    }

    /**
     * Resolve a status duration for the real-time simulator. New content should set durationMs directly;
     * legacy turn data is converted only as a temporary migration bridge.
     */
    public long resolvedDurationMs(long legacyTurnDurationMs) {
        if (durationMs > 0) return durationMs;
        if (durationTurns <= 0) return 0L;
        return Math.multiplyExact(durationTurns, legacyTurnDurationMs);
    }

    public long resolvedTickIntervalMs(long defaultTickIntervalMs) {
        return tickIntervalMs > 0 ? tickIntervalMs : defaultTickIntervalMs;
    }
}
