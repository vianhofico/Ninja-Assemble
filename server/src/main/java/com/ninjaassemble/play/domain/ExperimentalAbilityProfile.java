package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.battle.sim.BattleAbility;
import com.ninjaassemble.battle.sim.BattleAbilityKind;
import com.ninjaassemble.battle.sim.BattleAbilitySet;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.reference.ReferenceProfiles;
import org.springframework.stereotype.Component;

@Component
public final class ExperimentalAbilityProfile {
    public static final String VERSION = ReferenceProfiles.ABILITY_CYCLE;

    public BattleAbilitySet resolve(HeroContentCatalogService.HeroKitView kit) {
        if (kit == null || kit.techniques() == null || kit.techniques().size() < 4) throw new IllegalArgumentException("kit needs four executable techniques");
        return new BattleAbilitySet(
                map(kit.techniques().get(0), BattleAbilityKind.BASIC, 10_000, 30),
                map(kit.techniques().get(1), BattleAbilityKind.SKILL1, 12_500, 35),
                map(kit.techniques().get(2), BattleAbilityKind.SKILL2, 14_500, 35),
                map(kit.techniques().get(3), BattleAbilityKind.ULTIMATE, 22_000, -100));
    }

    private static BattleAbility map(HeroContentCatalogService.TechniqueView technique, BattleAbilityKind kind, int coefficientBps, int energyDelta) {
        DamageChannel channel = DamageChannel.valueOf(technique.channel());
        return new BattleAbility(
                technique.id(),
                kind,
                channel,
                coefficientBps,
                energyDelta,
                "vfx/techniques/" + technique.id());
    }
}
