package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;

public record BattleAbility(
        String id,
        BattleAbilityKind kind,
        DamageChannel channel,
        int coefficientBps,
        int energyDelta,
        String effectKey
) {
    public BattleAbility {
        if (id == null || id.isBlank() || kind == null || channel == null) throw new IllegalArgumentException("ability identity required");
        if (coefficientBps <= 0) throw new IllegalArgumentException("ability coefficient must be positive");
        if (energyDelta < -100 || energyDelta > 100) throw new IllegalArgumentException("invalid ability energy delta");
        if (effectKey == null || effectKey.isBlank()) effectKey = "vfx/techniques/" + id;
    }
}
