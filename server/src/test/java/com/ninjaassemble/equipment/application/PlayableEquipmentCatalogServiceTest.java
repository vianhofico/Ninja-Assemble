package com.ninjaassemble.equipment.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PlayableEquipmentCatalogServiceTest {
    @Test
    void packagedCatalogContainsFourFullSixSlotTiers() {
        PlayableEquipmentCatalogService catalog = new PlayableEquipmentCatalogService(null);
        assertEquals(24, catalog.all().size());
        assertEquals(6, catalog.all().stream().filter(it -> it.setId().equals("academy")).count());
        assertEquals(6, catalog.all().stream().filter(it -> it.setId().equals("jonin")).count());
        assertEquals(6, catalog.all().stream().filter(it -> it.setId().equals("kage")).count());
        assertEquals(6, catalog.all().stream().filter(it -> it.setId().equals("six-paths")).count());
        assertTrue(catalog.all().stream().allMatch(it -> it.maxEnhanceLevel() > 0));
    }
}
