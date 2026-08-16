package com.ninjaassemble.play.application;

import com.ninjaassemble.hero.catalog.HeroVersionAcquisitionCatalogService;
import com.ninjaassemble.reference.ReferenceProfiles;
import com.ninjaassemble.summon.domain.SummonBannerDefinition;
import com.ninjaassemble.summon.domain.SummonPoolEntry;
import com.ninjaassemble.summon.domain.SummonRarity;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CompleteRosterBannerFactory {
    public static final String BANNER_ID = "complete-roster";
    public static final String VERSION = ReferenceProfiles.COMPLETE_ROSTER_SUMMON;
    public static final long COST = 200;

    private final HeroVersionAcquisitionCatalogService catalog;

    public CompleteRosterBannerFactory(HeroVersionAcquisitionCatalogService catalog) {
        this.catalog = catalog;
    }

    public SummonBannerDefinition create() {
        List<SummonPoolEntry> pool = new ArrayList<>();
        for (var hero : catalog.summonable()) {
            SummonRarity rarity = SummonRarity.valueOf(hero.rarity());
            int weight = weightFor(rarity);
            boolean featured = rarity == SummonRarity.SSR || rarity == SummonRarity.UR;
            pool.add(new SummonPoolEntry(hero.heroId(), rarity, weight, featured));
        }
        if (pool.isEmpty()) throw new IllegalStateException("Complete Roster banner has no collectible Hero Versions");
        return new SummonBannerDefinition(BANNER_ID, VERSION, "DIAMOND", COST, 10, SummonRarity.SSR, pool);
    }

    static int weightFor(SummonRarity rarity) {
        return switch (rarity) {
            case UR -> 25;
            case SSR -> 150;
            case SR -> 700;
            case R -> 1_000;
        };
    }
}
