package com.ninjaassemble.play.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.hero.catalog.HeroVersionAcquisitionCatalogService;
import com.ninjaassemble.summon.domain.SummonRarity;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CompleteRosterBannerFactoryTest {
    @Test
    void bannerContainsOnlyApprovedSummonableHeroVersions() {
        HeroVersionAcquisitionCatalogService catalog = new HeroVersionAcquisitionCatalogService();
        CompleteRosterBannerFactory factory = new CompleteRosterBannerFactory(catalog);

        var banner = factory.create();
        Set<String> expectedHeroIds = catalog.summonable().stream()
                .map(HeroVersionAcquisitionCatalogService.HeroVersionAcquisitionEntry::heroId)
                .collect(Collectors.toSet());
        Set<String> awakeningIds = catalog.all().stream()
                .map(HeroVersionAcquisitionCatalogService.HeroVersionAcquisitionEntry::awakeningId)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());
        Set<String> actualHeroIds = banner.pool().stream().map(entry -> entry.heroId()).collect(Collectors.toSet());

        assertEquals(expectedHeroIds.size(), banner.pool().size());
        assertEquals(expectedHeroIds, actualHeroIds);
        assertTrue(banner.pool().stream().noneMatch(entry -> entry.heroId().contains("::")));
        assertTrue(actualHeroIds.stream().noneMatch(awakeningIds::contains));
        assertTrue(banner.pool().stream().allMatch(entry -> catalog.require(entry.heroId()).summonable()));
        assertFalse(banner.pool().isEmpty());
    }

    @Test
    void rarityWeightsComeFromHeroVersionRarityRatherThanVariantNames() {
        assertEquals(25, CompleteRosterBannerFactory.weightFor(SummonRarity.UR));
        assertEquals(150, CompleteRosterBannerFactory.weightFor(SummonRarity.SSR));
        assertEquals(700, CompleteRosterBannerFactory.weightFor(SummonRarity.SR));
        assertEquals(1_000, CompleteRosterBannerFactory.weightFor(SummonRarity.R));
    }
}
