package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;

public record BattleAbilitySet(
        BattleAbility basic,
        BattleAbility skill1,
        BattleAbility skill2,
        BattleAbility ultimate
) {
    public BattleAbilitySet {
        if (basic == null || skill1 == null || skill2 == null || ultimate == null) throw new IllegalArgumentException("four executable abilities are required");
    }

    public static BattleAbilitySet basicOnly(DamageChannel channel) {
        String id = channel == DamageChannel.CHAKRA ? "basic-chakra" : "basic-physical";
        return new BattleAbilitySet(
                new BattleAbility(id, BattleAbilityKind.BASIC, channel, 10_000, 30, "vfx/techniques/" + id),
                new BattleAbility(id, BattleAbilityKind.BASIC, channel, 10_000, 35, "vfx/techniques/" + id),
                new BattleAbility(id, BattleAbilityKind.BASIC, channel, 10_000, 35, "vfx/techniques/" + id),
                new BattleAbility(id, BattleAbilityKind.BASIC, channel, 10_000, -100, "vfx/techniques/" + id));
    }
}
