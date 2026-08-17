package com.ninjaassemble.hero.domain;

import com.ninjaassemble.battle.domain.DamageChannel;

/**
 * A machine-readable combat effect. All persistent timing is expressed in logical milliseconds.
 * Instant effects use durationMs=0 and tickIntervalMs=0.
 */
public record SkillEffectDefinition(
        EffectType type,
        TargetSelector target,
        DamageChannel channel,
        int coefficientBps,
        long flatAmount,
        String status,
        int chanceBps,
        long durationMs,
        long tickIntervalMs
) {
    public SkillEffectDefinition {
        if (type == null || target == null) throw new IllegalArgumentException("effect type/target required");
        if (coefficientBps < 0 || flatAmount < 0) throw new IllegalArgumentException("effect values cannot be negative");
        if (chanceBps < 0 || chanceBps > 10_000) throw new IllegalArgumentException("invalid effect chance");
        if (durationMs < 0) throw new IllegalArgumentException("durationMs cannot be negative");
        if (tickIntervalMs < 0) throw new IllegalArgumentException("tickIntervalMs cannot be negative");
        if (tickIntervalMs > 0 && durationMs <= 0) throw new IllegalArgumentException("periodic effect requires durationMs > 0");
        if (durationMs > 0 && status == null && type == EffectType.STATUS) throw new IllegalArgumentException("timed status requires status id");
    }

    /** Compatibility constructor for instant effects only. Persistent callers must pass explicit milliseconds. */
    public SkillEffectDefinition(EffectType type, TargetSelector target, DamageChannel channel, int coefficientBps,
                                 long flatAmount, String status, int chanceBps) {
        this(type, target, channel, coefficientBps, flatAmount, status, chanceBps, 0L, 0L);
    }

    public boolean periodic() { return tickIntervalMs > 0; }
    public boolean persistent() { return durationMs > 0; }
}
