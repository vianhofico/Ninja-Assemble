package com.ninjaassemble.shop.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.inventory.application.ItemCatalogService;
import org.junit.jupiter.api.Test;

class ShopCatalogServiceTest {
    @Test
    void designCatalogContainsThreeValidatedDailyShopsAndEightOffers() {
        ShopCatalogService catalog = new ShopCatalogService(new ItemCatalogService());
        assertEquals(3, catalog.all().size());
        assertEquals(8, catalog.all().stream().mapToInt(shop -> shop.definition().offers().size()).sum());
        assertTrue(catalog.all().stream().allMatch(shop -> "DAILY_05".equals(shop.definition().refreshProfile())));
        assertEquals("summon-ticket", catalog.requireOffer("general", "summon-ticket-gold").offer().itemDefinitionId());
        assertEquals("ARENA_COIN", catalog.requireOffer("arena", "elite-seal-arena").offer().currency());
    }
}
