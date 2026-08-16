package com.ninjaassemble.hero.catalog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HeroCatalogServiceTest {
    @Test
    void catalogContainsCoreFranchisePillars() {
        HeroCatalogService catalog = new HeroCatalogService();
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("naruto-uzumaki")));
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("sasuke-uchiha")));
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("madara-uchiha")));
        assertTrue(catalog.all().stream().anyMatch(it -> it.id().equals("kaguya-otsutsuki")));
        assertTrue(catalog.all().size() >= 60);
    }
}
