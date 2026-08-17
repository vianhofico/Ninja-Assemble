package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.battle.sim.BattlePassive;
import com.ninjaassemble.battle.sim.PassiveTrigger;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import com.ninjaassemble.reference.ReferenceProfiles;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public final class PassiveEffectResolver {
    public static final String VERSION = ReferenceProfiles.PASSIVE_LIFECYCLE;

    public BattlePassive resolve(HeroContentCatalogService.TechniqueView technique) {
        if (technique == null || !"PASSIVE".equals(technique.kind())) throw new IllegalArgumentException("passive technique required");
        DamageChannel channel = DamageChannel.valueOf(technique.channel());
        return switch (technique.id()) {
            case "copy-tactics" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 1_200, 8_000));
            case "passive-byakugan" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    status(TargetSelector.SELF, channel, "SPEED_UP", 1_200, 10_000),
                    status(TargetSelector.SELF, channel, "ATK_UP", 1_000, 10_000));
            case "passive-immortal" -> passive(technique.id(), PassiveTrigger.HP_THRESHOLD, true, 3_500, 0,
                    heal(TargetSelector.SELF, channel, 8_000));
            case "passive-jinchuriki" -> passive(technique.id(), PassiveTrigger.HP_THRESHOLD, true, 4_000, 0,
                    shield(TargetSelector.SELF, channel, 10_000), status(TargetSelector.SELF, channel, "ATK_UP", 2_500, 8_000));
            case "passive-medical" -> passive(technique.id(), PassiveTrigger.TIME_INTERVAL, false, 0, 3_000,
                    heal(TargetSelector.LOWEST_HP_ALLY, channel, 1_500));
            case "passive-puppet" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    shield(TargetSelector.SELF, channel, 6_000));
            case "passive-rinnegan" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    rage(TargetSelector.SELF, 30));
            case "passive-sage" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 1_800, 10_000),
                    status(TargetSelector.SELF, channel, "SPEED_UP", 1_000, 10_000));
            case "passive-sharingan" -> passive(technique.id(), PassiveTrigger.AFTER_DAMAGE_TAKEN, false, 0, 0,
                    rage(TargetSelector.SELF, 10));
            case "passive-strategist" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    status(TargetSelector.ALL_ALLIES, channel, "SPEED_UP", 800, 8_000));
            case "passive-swordsman" -> passive(technique.id(), PassiveTrigger.AFTER_DAMAGE_DEALT, false, 0, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 500, 4_000));
            case "passive-will-of-fire" -> passive(technique.id(), PassiveTrigger.ALLY_KO, false, 0, 0,
                    status(TargetSelector.ALL_ALLIES, channel, "ATK_UP", 1_200, 7_000));
            default -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 800, 6_000));
        };
    }

    private static BattlePassive passive(String id, PassiveTrigger trigger, boolean once, int thresholdBps,
                                         long intervalMs, SkillEffectDefinition... effects) {
        return new BattlePassive(id, trigger, List.of(effects), once, thresholdBps, intervalMs);
    }

    private static SkillEffectDefinition status(TargetSelector target, DamageChannel channel, String status,
                                                int modifierBps, long durationMs) {
        return new SkillEffectDefinition(EffectType.STATUS, target, channel, modifierBps, 0, status, 10_000, durationMs, 0);
    }

    private static SkillEffectDefinition heal(TargetSelector target, DamageChannel channel, int coefficientBps) {
        return new SkillEffectDefinition(EffectType.HEAL, target, channel, coefficientBps, 0, null, 10_000);
    }

    private static SkillEffectDefinition shield(TargetSelector target, DamageChannel channel, int coefficientBps) {
        return new SkillEffectDefinition(EffectType.SHIELD, target, channel, coefficientBps, 0, null, 10_000);
    }

    private static SkillEffectDefinition rage(TargetSelector target, long amount) {
        return new SkillEffectDefinition(EffectType.RAGE, target, null, 0, amount, null, 10_000);
    }
}
