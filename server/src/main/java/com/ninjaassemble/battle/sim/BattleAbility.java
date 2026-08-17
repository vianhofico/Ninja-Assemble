package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import java.util.List;

/**
 * Executable battle ability plus deterministic real-time timing metadata.
 *
 * <p>The legacy constructors are intentionally retained while the existing turn-based callers are migrated.
 * New real-time code should use {@link #cooldownMs()}, {@link #castTimeMs()} and {@link #recoveryMs()} rather
 * than inferring timing from rounds.</p>
 */
public record BattleAbility(
        String id,
        BattleAbilityKind kind,
        DamageChannel channel,
        int coefficientBps,
        int energyDelta,
        String effectKey,
        List<SkillEffectDefinition> effects,
        long cooldownMs,
        long castTimeMs,
        long recoveryMs
) {
    public BattleAbility {
        if (id == null || id.isBlank() || kind == null || channel == null) throw new IllegalArgumentException("ability identity required");
        if (coefficientBps <= 0) throw new IllegalArgumentException("ability coefficient must be positive");
        if (energyDelta < -100 || energyDelta > 100) throw new IllegalArgumentException("invalid ability energy delta");
        if (cooldownMs < 0 || castTimeMs < 0 || recoveryMs < 0) throw new IllegalArgumentException("ability timing cannot be negative");
        if (effectKey == null || effectKey.isBlank()) effectKey = "vfx/techniques/" + id;
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    /**
     * Compatibility constructor for existing content. Timing defaults are deterministic and intentionally
     * conservative; content can opt into measured timings through the canonical constructor.
     */
    public BattleAbility(
            String id,
            BattleAbilityKind kind,
            DamageChannel channel,
            int coefficientBps,
            int energyDelta,
            String effectKey,
            List<SkillEffectDefinition> effects
    ) {
        this(id, kind, channel, coefficientBps, energyDelta, effectKey, effects,
                defaultCooldownMs(kind), defaultCastTimeMs(kind), defaultRecoveryMs(kind));
    }

    public BattleAbility(String id, BattleAbilityKind kind, DamageChannel channel, int coefficientBps, int energyDelta, String effectKey) {
        this(id, kind, channel, coefficientBps, energyDelta, effectKey, List.of());
    }

    private static long defaultCooldownMs(BattleAbilityKind kind) {
        return switch (kind) {
            case BASIC, PASSIVE -> 0L;
            case SKILL1 -> 5_000L;
            case SKILL2 -> 7_000L;
            case ULTIMATE -> 10_000L;
        };
    }

    private static long defaultCastTimeMs(BattleAbilityKind kind) {
        return switch (kind) {
            case BASIC, PASSIVE -> 0L;
            case SKILL1, SKILL2 -> 300L;
            case ULTIMATE -> 550L;
        };
    }

    private static long defaultRecoveryMs(BattleAbilityKind kind) {
        return switch (kind) {
            case PASSIVE -> 0L;
            case BASIC -> 150L;
            case SKILL1, SKILL2 -> 250L;
            case ULTIMATE -> 400L;
        };
    }
}
