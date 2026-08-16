package com.ninjaassemble.play.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.hero.catalog.VariantCatalogService;
import com.ninjaassemble.summon.domain.SummonRarity;
import org.junit.jupiter.api.Test;

class CompleteRosterBannerFactoryTest {
    @Test
    void bannerContainsEveryBaseCharacterAndVariant() {
        CompleteRosterBannerFactory factory = new CompleteRosterBannerFactory(new HeroCatalogService(), new VariantCatalogService());
        var banner = factory.create();
        assertEquals(189 + 427, banner.pool().size());
        assertTrue(banner.pool().stream().anyMatch(it -> it.heroVariantId().equals("naruto-uzumaki::Six Paths Sage Mode")));
        assertEquals(SummonRarity.UR, CompleteRosterBannerFactory.rarityFor("Six Paths Sage Mode"));
        assertEquals(SummonRarity.SSR, CompleteRosterBannerFactory.rarityFor("Sage Mode"));
    }
}
