package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import java.util.List;

public record BattleAbility(
        String id,
        BattleAbilityKind kind,
        DamageChannel channel,
        int coefficientBps,
        int energyDelta,
        String effectKey,
        List<SkillEffectDefinition> effects
) {
    public BattleAbility {
        if (id == null || id.isBlank() || kind == null || channel == null) throw new IllegalArgumentException("ability identity required");
        if (coefficientBps <= 0) throw new IllegalArgumentException("ability coefficient must be positive");
        if (energyDelta < -100 || energyDelta > 100) throw new IllegalArgumentException("invalid ability energy delta");
        if (effectKey == null || effectKey.isBlank()) effectKey = "vfx/techniques/" + id;
        effects = effects == null ? List.of() : List.copyOf(effects);
    }

    public BattleAbility(String id, BattleAbilityKind kind, DamageChannel channel, int coefficientBps, int energyDelta, String effectKey) {
        this(id, kind, channel, coefficientBps, energyDelta, effectKey, List.of());
    }
}
