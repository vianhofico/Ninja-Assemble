package com.ninjaassemble.play.application;

import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.hero.catalog.HeroVariantEntry;
import com.ninjaassemble.hero.catalog.VariantCatalogService;
import com.ninjaassemble.reference.ReferenceProfiles;
import com.ninjaassemble.summon.domain.SummonBannerDefinition;
import com.ninjaassemble.summon.domain.SummonPoolEntry;
import com.ninjaassemble.summon.domain.SummonRarity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CompleteRosterBannerFactory {
    public static final String BANNER_ID = "complete-roster";
    public static final String VERSION = ReferenceProfiles.COMPLETE_ROSTER_SUMMON;
    public static final long COST = 200;
    private final HeroCatalogService catalog;
    private final VariantCatalogService variants;

    public CompleteRosterBannerFactory(HeroCatalogService catalog, VariantCatalogService variants) {
        this.catalog = catalog;
        this.variants = variants;
    }

    public SummonBannerDefinition create() {
        List<SummonPoolEntry> pool = new ArrayList<>();
        catalog.all().forEach(hero -> pool.add(new SummonPoolEntry(hero.id() + "::BASE", SummonRarity.SR, 1_000, false)));
        for (HeroVariantEntry variant : variants.all()) {
            SummonRarity rarity = rarityFor(variant.variant());
            int weight = rarity == SummonRarity.UR ? 25 : rarity == SummonRarity.SSR ? 150 : 700;
            pool.add(new SummonPoolEntry(variant.characterId() + "::" + variant.variant(), rarity, weight, rarity != SummonRarity.SR));
        }
        return new SummonBannerDefinition(BANNER_ID, VERSION, "DIAMOND", COST, 10, SummonRarity.SSR, pool);
    }

    public static SummonRarity rarityFor(String variant) {
        String v = variant == null ? "" : variant.toLowerCase(Locale.ROOT);
        if (containsAny(v, "six paths", "ten-tails", "rabbit goddess", "tengai", "rinnegan", "eight gate", "tenseigan chakra", "bijuu mode")) return SummonRarity.UR;
        if (containsAny(v, "sage", "mangekyo", "susanoo", "kcm", "cloak", "edo tensei", "reanimated", "curse mark level 2", "hundred", "raikage", "kazekage", "mizukage", "tsuchikage", "hokage")) return SummonRarity.SSR;
        return SummonRarity.SR;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
