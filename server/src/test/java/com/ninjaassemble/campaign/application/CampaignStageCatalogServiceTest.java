package com.ninjaassemble.campaign.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.campaign.domain.CampaignDifficulty;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.inventory.application.ItemCatalogService;
import java.util.List;
import org.junit.jupiter.api.Test;

class CampaignStageCatalogServiceTest {
    private final CampaignStageCatalogService catalog = new CampaignStageCatalogService(new HeroCatalogService(), new ItemCatalogService());

    @Test
    void catalogContainsTwelveOrderedPlayableStagesAcrossThreeChapters() {
        assertEquals(12, catalog.size());
        List<CampaignStageCatalogService.CampaignStageEntry> stages = catalog.all();
        assertEquals("c1-s1", stages.get(0).stage().id());
        assertEquals("c3-s4", stages.get(11).stage().id());
        assertEquals(CampaignDifficulty.NORMAL, stages.get(0).stage().difficulty());
        assertEquals(CampaignDifficulty.ELITE, stages.get(4).stage().difficulty());
        assertEquals(CampaignDifficulty.HEROIC, stages.get(8).stage().difficulty());
        for (var entry : stages) {
            assertTrue(entry.stage().waves().size() >= 1);
            for (var wave : entry.stage().waves()) assertEquals(5, wave.enemies().size());
        }
    }

    @Test
    void bossStagesUseTwoWavesAndRegularStagesUseOne() {
        assertEquals(2, catalog.require("c1-s4").stage().waves().size());
        assertEquals(2, catalog.require("c2-s4").stage().waves().size());
        assertEquals(2, catalog.require("c3-s4").stage().waves().size());
        assertEquals(1, catalog.require("c3-s3").stage().waves().size());
    }

    @Test
    void prerequisiteChainCurrencyAndItemRewardsAreDataDriven() {
        var first = catalog.require("c1-s1").stage();
        var second = catalog.require("c1-s2").stage();
        var boss = catalog.require("c3-s4").stage();
        assertTrue(first.prerequisiteStageIds().isEmpty());
        assertEquals(List.of("c1-s1"), second.prerequisiteStageIds().stream().toList());
        assertEquals(15, boss.energyCost());
        assertEquals(6, boss.minPlayerLevel());
        assertEquals(5_000L, boss.firstClearReward().currencies().get("GOLD"));
        assertEquals(50L, boss.firstClearReward().currencies().get("DIAMOND"));
        assertEquals(3L, boss.firstClearReward().items().get("summon-ticket"));
        assertEquals(6L, boss.firstClearReward().items().get("heroic-token"));
        assertEquals(2_500L, boss.repeatReward().currencies().get("GOLD"));
        assertEquals(2L, boss.repeatReward().items().get("heroic-token"));
        assertTrue(boss.firstClearReward().playerExp() > boss.repeatReward().playerExp());
    }

    @Test
    void localizedStageNamesArePresent() {
        var stage = catalog.require("c1-s4");
        assertEquals("Sand Vanguard", stage.nameEn());
        assertEquals("Tiên phong Làng Cát", stage.nameVi());
    }
}
