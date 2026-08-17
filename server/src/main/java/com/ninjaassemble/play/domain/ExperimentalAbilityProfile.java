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
    private final TechniqueEffectResolver effects;

    public ExperimentalAbilityProfile() {
        this(new TechniqueEffectResolver());
    }

    ExperimentalAbilityProfile(TechniqueEffectResolver effects) {
        this.effects = effects;
    }

    public BattleAbilitySet resolve(HeroContentCatalogService.HeroKitView kit) {
        if (kit == null || kit.techniques() == null || kit.techniques().size() < 4) throw new IllegalArgumentException("kit needs four executable techniques");
        return new BattleAbilitySet(
                map(kit.techniques().get(0), BattleAbilityKind.BASIC, 10_000, 30),
                map(kit.techniques().get(1), BattleAbilityKind.SKILL1, 12_500, 35),
                map(kit.techniques().get(2), BattleAbilityKind.SKILL2, 14_500, 35),
                map(kit.techniques().get(3), BattleAbilityKind.ULTIMATE, 22_000, -100));
    }

    private BattleAbility map(HeroContentCatalogService.TechniqueView technique, BattleAbilityKind kind, int coefficientBps, int energyDelta) {
        DamageChannel channel = DamageChannel.valueOf(technique.channel());
        TechniqueEffectResolver.Resolution resolution = effects.resolve(technique);
        if (resolution.status() != TechniqueEffectResolver.MappingStatus.RUNTIME || resolution.effects().isEmpty())
            throw new IllegalStateException("executable technique has no runtime effect mapping: " + technique.id());
        AbilityTiming timing = timing(kind);
        return new BattleAbility(
                technique.id(),
                kind,
                channel,
                coefficientBps,
                energyDelta,
                "vfx/techniques/" + technique.id(),
                resolution.effects(),
                timing.cooldownMs(),
                timing.castTimeMs(),
                timing.recoveryMs());
    }

    private static AbilityTiming timing(BattleAbilityKind kind) {
        return switch (kind) {
            case BASIC -> new AbilityTiming(0L, 0L, 150L);
            case SKILL1 -> new AbilityTiming(5_000L, 300L, 250L);
            case SKILL2 -> new AbilityTiming(7_000L, 300L, 250L);
            case ULTIMATE -> new AbilityTiming(10_000L, 550L, 400L);
            case PASSIVE -> new AbilityTiming(0L, 0L, 0L);
        };
    }

    private record AbilityTiming(long cooldownMs, long castTimeMs, long recoveryMs) {}
}
