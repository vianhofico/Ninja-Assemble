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
    private static final long LONG_BATTLE_BUFF_MS = 150_000L;
    private static final long SHORT_BUFF_MS = 6_000L;
    private static final long MEDIUM_BUFF_MS = 9_000L;

    public BattlePassive resolve(HeroContentCatalogService.TechniqueView technique) {
        if (technique == null || !"PASSIVE".equals(technique.kind())) throw new IllegalArgumentException("passive technique required");
        DamageChannel channel = DamageChannel.valueOf(technique.channel());
        return switch (technique.id()) {
            case "copy-tactics" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 1_200, LONG_BATTLE_BUFF_MS));
            case "passive-byakugan" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    status(TargetSelector.SELF, channel, "SPEED_UP", 1_200, LONG_BATTLE_BUFF_MS),
                    status(TargetSelector.SELF, channel, "ATK_UP", 1_000, LONG_BATTLE_BUFF_MS));
            case "passive-immortal" -> passive(technique.id(), PassiveTrigger.SELF_LOW_HP, true, 3_500,
                    heal(TargetSelector.SELF, channel, 8_000));
            case "passive-jinchuriki" -> passive(technique.id(), PassiveTrigger.SELF_LOW_HP, true, 4_000,
                    shield(TargetSelector.SELF, channel, 10_000),
                    status(TargetSelector.SELF, channel, "ATK_UP", 2_500, MEDIUM_BUFF_MS));
            case "passive-medical" -> passive(technique.id(), PassiveTrigger.TURN_START, false, 0,
                    heal(TargetSelector.LOWEST_HP_ALLY, channel, 1_500));
            case "passive-puppet" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    shield(TargetSelector.SELF, channel, 6_000));
            case "passive-rinnegan" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    energy(TargetSelector.SELF, 30));
            case "passive-sage" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 1_800, LONG_BATTLE_BUFF_MS),
                    status(TargetSelector.SELF, channel, "SPEED_UP", 1_000, LONG_BATTLE_BUFF_MS));
            case "passive-sharingan" -> passive(technique.id(), PassiveTrigger.AFTER_DAMAGE_TAKEN, false, 0,
                    energy(TargetSelector.SELF, 10));
            case "passive-strategist" -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    status(TargetSelector.ALL_ALLIES, channel, "SPEED_UP", 800, LONG_BATTLE_BUFF_MS));
            case "passive-swordsman" -> passive(technique.id(), PassiveTrigger.AFTER_DAMAGE_DEALT, false, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 500, SHORT_BUFF_MS));
            case "passive-will-of-fire" -> passive(technique.id(), PassiveTrigger.ALLY_KO, false, 0,
                    status(TargetSelector.ALL_ALLIES, channel, "ATK_UP", 1_200, MEDIUM_BUFF_MS));
            default -> passive(technique.id(), PassiveTrigger.BATTLE_START, true, 0,
                    status(TargetSelector.SELF, channel, "ATK_UP", 800, LONG_BATTLE_BUFF_MS));
        };
    }

    private static BattlePassive passive(String id, PassiveTrigger trigger, boolean once, int thresholdBps, SkillEffectDefinition... effects) {
        return new BattlePassive(id, trigger, List.of(effects), once, thresholdBps);
    }

    private static SkillEffectDefinition status(TargetSelector target, DamageChannel channel, String status, int modifierBps, long durationMs) {
        return effect(EffectType.STATUS, target, channel, modifierBps, 0, status, durationMs, 0L);
    }

    private static SkillEffectDefinition heal(TargetSelector target, DamageChannel channel, int coefficientBps) {
        return effect(EffectType.HEAL, target, channel, coefficientBps, 0, null, 0L, 0L);
    }

    private static SkillEffectDefinition shield(TargetSelector target, DamageChannel channel, int coefficientBps) {
        return effect(EffectType.SHIELD, target, channel, coefficientBps, 0, null, 0L, 0L);
    }

    private static SkillEffectDefinition energy(TargetSelector target, long amount) {
        return effect(EffectType.ENERGY, target, null, 0, amount, null, 0L, 0L);
    }

    private static SkillEffectDefinition effect(
            EffectType type,
            TargetSelector target,
            DamageChannel channel,
            int coefficientBps,
            long flatAmount,
            String status,
            long durationMs,
            long tickIntervalMs
    ) {
        return new SkillEffectDefinition(type, target, channel, coefficientBps, flatAmount, status, 10_000, 0, durationMs, tickIntervalMs);
    }
}
