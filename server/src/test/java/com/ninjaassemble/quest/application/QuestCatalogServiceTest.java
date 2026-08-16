package com.ninjaassemble.quest.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.inventory.application.ItemCatalogService;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestCatalogServiceTest {
    @Test
    void dailyCatalogUsesFourServerAuditableMetricsAndExistingItems() {
        QuestCatalogService catalog = new QuestCatalogService(new ItemCatalogService());
        assertEquals(4, catalog.all().size());
        assertEquals(Set.of("CAMPAIGN_CLEAR", "ARENA_BATTLE", "SUMMON", "HERO_LEVEL_UP"),
                catalog.all().stream().map(it -> it.metric().name()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(catalog.all().stream().allMatch(it -> it.target() == 1));
        assertEquals("summon-ticket", catalog.require("daily-summon").rewardItemId());
    }
}
