package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.List;

/** Four executable pre-Awakening actions. The former ultimate slot is the Rage Skill. */
public record BattleAbilitySet(
        BattleAbility basic,
        BattleAbility skill1,
        BattleAbility skill2,
        BattleAbility rageSkill
) {
    public BattleAbilitySet {
        if (basic == null || skill1 == null || skill2 == null || rageSkill == null) throw new IllegalArgumentException("complete ability set required");
    }

    public static BattleAbilitySet basicOnly(DamageChannel channel) {
        SkillEffectDefinition damage = new SkillEffectDefinition(
                EffectType.DAMAGE, TargetSelector.FRONTMOST_ENEMY, channel, 10_000, 0, null, 10_000);
        BattleAbility basic = new BattleAbility("basic", BattleAbilityKind.BASIC, channel, 10_000, 15, "vfx/basic", List.of(damage), 0, 0, 250);
        BattleAbility skill1 = new BattleAbility("basic-s1", BattleAbilityKind.SKILL1, channel, 11_000, 0, "vfx/basic", List.of(damage), 6_000, 0, 400);
        BattleAbility skill2 = new BattleAbility("basic-s2", BattleAbilityKind.SKILL2, channel, 12_000, 0, "vfx/basic", List.of(damage), 9_000, 0, 450);
        BattleAbility rage = new BattleAbility("basic-rage", BattleAbilityKind.RAGE_SKILL, channel, 18_000, -100, "vfx/basic-rage", List.of(damage), 0, 500, 700);
        return new BattleAbilitySet(basic, skill1, skill2, rage);
    }

    /** @deprecated Use rageSkill(). */
    @Deprecated(forRemoval = true)
    public BattleAbility ultimate() { return rageSkill; }
}
