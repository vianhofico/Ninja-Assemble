package com.ninjaassemble.hero.domain;

import com.ninjaassemble.battle.domain.DamageChannel;

public record SkillEffectDefinition(
        EffectType type,
        TargetSelector target,
        DamageChannel channel,
        int coefficientBps,
        long flatAmount,
        String status,
        int chanceBps,
        int durationTurns
) {
    public SkillEffectDefinition {
        if (type == null || target == null) throw new IllegalArgumentException("effect type/target required");
        if (coefficientBps < 0 || chanceBps < 0 || chanceBps > 10_000 || durationTurns < 0) throw new IllegalArgumentException("invalid effect values");
    }
}
