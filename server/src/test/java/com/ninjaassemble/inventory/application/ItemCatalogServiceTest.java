package com.ninjaassemble.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.inventory.domain.ItemType;
import org.junit.jupiter.api.Test;

class ItemCatalogServiceTest {
    private final ItemCatalogService catalog = new ItemCatalogService();

    @Test
    void campaignStackItemCatalogIsSmallTypedAndLocalized() {
        assertEquals(5, catalog.size());
        assertEquals(ItemType.SCROLL, catalog.require("training-scroll").type());
        assertEquals(ItemType.MATERIAL, catalog.require("chakra-crystal").type());
        assertEquals(ItemType.SUMMON_TICKET, catalog.require("summon-ticket").type());
        assertTrue(catalog.all().stream().allMatch(item -> !item.nameEn().isBlank() && !item.nameVi().isBlank()));
        assertTrue(catalog.all().stream().allMatch(item -> "DESIGN_BASELINE".equals(item.status())));
    }
}
