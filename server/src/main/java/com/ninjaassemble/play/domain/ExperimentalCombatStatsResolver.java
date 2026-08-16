package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.equipment.application.EquipmentApplicationService;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.reference.ReferenceProfiles;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExperimentalCombatStatsResolver {
    public static final String VERSION = ReferenceProfiles.COMBAT_STATS + "+" + EquipmentApplicationService.COMBAT_BONUS_VERSION;
    private final HeroContentCatalogService content;
    private final ExperimentalAbilityProfile abilities;
    private final PassiveEffectResolver passiveResolver;
    private final EquipmentApplicationService equipment;

    @Autowired
    public ExperimentalCombatStatsResolver(HeroContentCatalogService content, ExperimentalAbilityProfile abilities,
                                           PassiveEffectResolver passiveResolver, EquipmentApplicationService equipment) {
        this.content = content;
        this.abilities = abilities;
        this.passiveResolver = passiveResolver;
        this.equipment = equipment;
    }

    ExperimentalCombatStatsResolver(HeroContentCatalogService content, ExperimentalAbilityProfile abilities,
                                    PassiveEffectResolver passiveResolver) {
        this.content = content;
        this.abilities = abilities;
        this.passiveResolver = passiveResolver;
        this.equipment = null;
    }

    public BattleUnitSeed resolve(String battleUnitId, String characterId, String variant, int level, TeamSide side, int slot) {
        if (level < 1) throw new IllegalArgumentException("level must be positive");
        var kit = content.resolve(characterId, variant);
        DamageChannel channel = DamageChannel.valueOf(kit.techniques().get(0).channel());
        int hash = Math.floorMod((characterId + "::" + (variant == null ? "BASE" : variant)).hashCode(), 10_000);
        long hp = 1_200L + level * 140L + hash % 401;
        long primary = 160L + level * 24L + hash % 61;
        long secondary = 120L + level * 18L + (hash / 7) % 51;
        long pAtk = channel == DamageChannel.PHYSICAL ? primary : secondary;
        long cAtk = channel == DamageChannel.CHAKRA ? primary : secondary;
        long pDef = 80L + level * 10L + (hash / 11) % 31;
        long cDef = 80L + level * 10L + (hash / 13) % 31;
        int speed = 90 + hash % 31;
        int crit = 800 + hash % 1_201;
        BattleUnitSeed base = new BattleUnitSeed(
                battleUnitId, side, slot, hp, pAtk, cAtk, pDef, cDef, speed, crit, crit, channel,
                abilities.resolve(kit), List.of(passiveResolver.resolve(kit.techniques().get(4))));
        UUID playerHeroId = playerHeroId(battleUnitId);
        return equipment == null || playerHeroId == null ? base : equipment.applyCombatBonus(playerHeroId, base);
    }

    private static UUID playerHeroId(String battleUnitId) {
        if (battleUnitId == null || battleUnitId.isBlank()) return null;
        int separator = battleUnitId.lastIndexOf(':');
        String candidate = separator >= 0 ? battleUnitId.substring(separator + 1) : battleUnitId;
        try { return UUID.fromString(candidate); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
