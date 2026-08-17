package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import java.util.List;

/** Executable ability with real-time timing and Rage semantics. */
public record BattleAbility(
        String id,
        BattleAbilityKind kind,
        DamageChannel channel,
        int coefficientBps,
        int rageDelta,
        String effectKey,
        List<SkillEffectDefinition> effects,
        long cooldownMs,
        long castTimeMs,
        long recoveryMs
) {
    public BattleAbility {
        if (id == null || id.isBlank() || kind == null || channel == null) throw new IllegalArgumentException("ability identity required");
        if (coefficientBps < 0) throw new IllegalArgumentException("coefficient cannot be negative");
        if (rageDelta < -100 || rageDelta > 100) throw new IllegalArgumentException("rage delta outside -100..100");
        if (effectKey == null || effectKey.isBlank()) throw new IllegalArgumentException("effect key required");
        effects = effects == null ? List.of() : List.copyOf(effects);
        if (effects.isEmpty()) throw new IllegalArgumentException("ability needs effects");
        if (cooldownMs < 0 || castTimeMs < 0 || recoveryMs < 0) throw new IllegalArgumentException("ability timings cannot be negative");
    }

    public BattleAbility(String id, BattleAbilityKind kind, DamageChannel channel, int coefficientBps,
                         int rageDelta, String effectKey, List<SkillEffectDefinition> effects) {
        this(id, kind, channel, coefficientBps, rageDelta, effectKey, effects,
                defaultCooldown(kind), defaultCast(kind), defaultRecovery(kind));
    }

    private static long defaultCooldown(BattleAbilityKind kind) {
        return switch (kind) {
            case SKILL1 -> 6_000L;
            case SKILL2 -> 9_000L;
            case AWAKENING_SKILL -> 12_000L;
            default -> 0L;
        };
    }

    private static long defaultCast(BattleAbilityKind kind) {
        return switch (kind) {
            case RAGE_SKILL, ULTIMATE -> 600L;
            case AWAKENING_SKILL -> 800L;
            default -> 0L;
        };
    }

    private static long defaultRecovery(BattleAbilityKind kind) {
        return switch (kind) {
            case BASIC -> 250L;
            case SKILL1, SKILL2 -> 450L;
            case RAGE_SKILL, ULTIMATE -> 750L;
            case AWAKENING_SKILL -> 900L;
            case PASSIVE, CONDITIONAL_SKILL -> 350L;
        };
    }

    /** Temporary Java compatibility accessor for old consumers; the resource is Rage from M49 onward. */
    @Deprecated(forRemoval = true)
    public int energyDelta() { return rageDelta; }
}
