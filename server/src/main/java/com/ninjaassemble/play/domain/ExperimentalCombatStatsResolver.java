package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.reference.ReferenceProfiles;
import org.springframework.stereotype.Component;

@Component
public class ExperimentalCombatStatsResolver {
    public static final String VERSION = ReferenceProfiles.COMBAT_STATS;
    private final HeroContentCatalogService content;

    public ExperimentalCombatStatsResolver(HeroContentCatalogService content) {
        this.content = content;
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
        return new BattleUnitSeed(battleUnitId, side, slot, hp, pAtk, cAtk, pDef, cDef, speed, crit, crit, channel);
    }
}
