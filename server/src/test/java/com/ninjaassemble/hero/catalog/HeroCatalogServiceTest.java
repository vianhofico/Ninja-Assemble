package com.ninjaassemble.hero.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HeroCatalogServiceTest {
    @Test
    void packagedCatalogContainsTheCompleteBaseRoster() {
        HeroCatalogService catalog = new HeroCatalogService();
        assertEquals(189, catalog.all().size());
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("naruto-uzumaki")));
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("madara-uchiha")));
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("kaguya-otsutsuki")));
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("toneri-otsutsuki")));
    }
}
