package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.battle.sim.BattleAbility;
import com.ninjaassemble.battle.sim.BattleAbilityKind;
import com.ninjaassemble.battle.sim.BattleAbilitySet;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.reference.ReferenceProfiles;
import org.springframework.stereotype.Component;

/** Experimental timing/Rage defaults. M50 replaces per-Hero Version identity/tuning after M49 runtime stabilizes. */
@Component
public final class ExperimentalAbilityProfile {
    public static final String VERSION = ReferenceProfiles.ABILITY_CYCLE;
    private final TechniqueEffectResolver effects;

    public ExperimentalAbilityProfile() { this(new TechniqueEffectResolver()); }
    ExperimentalAbilityProfile(TechniqueEffectResolver effects) { this.effects = effects; }

    public BattleAbilitySet resolve(HeroContentCatalogService.HeroKitView kit) {
        if (kit == null || kit.techniques() == null || kit.techniques().size() < 4) throw new IllegalArgumentException("kit needs four executable techniques");
        return new BattleAbilitySet(
                map(kit.techniques().get(0), BattleAbilityKind.BASIC, 10_000, 15, 0, 0, 250),
                map(kit.techniques().get(1), BattleAbilityKind.SKILL1, 12_500, 0, 6_000, 250, 450),
                map(kit.techniques().get(2), BattleAbilityKind.SKILL2, 14_500, 0, 9_000, 350, 500),
                map(kit.techniques().get(3), BattleAbilityKind.RAGE_SKILL, 22_000, -100, 0, 650, 800));
    }

    private BattleAbility map(HeroContentCatalogService.TechniqueView technique, BattleAbilityKind kind,
                              int coefficientBps, int rageDelta, long cooldownMs, long castTimeMs, long recoveryMs) {
        DamageChannel channel = DamageChannel.valueOf(technique.channel());
        TechniqueEffectResolver.Resolution resolution = effects.resolve(technique);
        if (resolution.status() != TechniqueEffectResolver.MappingStatus.RUNTIME || resolution.effects().isEmpty())
            throw new IllegalStateException("executable technique has no runtime effect mapping: " + technique.id());
        return new BattleAbility(technique.id(), kind, channel, coefficientBps, rageDelta,
                "vfx/techniques/" + technique.id(), resolution.effects(), cooldownMs, castTimeMs, recoveryMs);
    }
}
