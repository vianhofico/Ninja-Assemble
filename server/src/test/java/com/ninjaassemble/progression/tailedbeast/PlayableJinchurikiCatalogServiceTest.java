package com.ninjaassemble.progression.tailedbeast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PlayableJinchurikiCatalogServiceTest {
    @Test
    void flagshipHostsResolveToTheirTailedBeasts() {
        PlayableJinchurikiCatalogService catalog = new PlayableJinchurikiCatalogService();
        assertTrue(catalog.all().size() >= 10);
        assertEquals("KURAMA", catalog.require("naruto-uzumaki").beast());
        assertEquals("GYUKI", catalog.require("killer-b").beast());
        assertEquals("SHUKAKU", catalog.require("gaara").beast());
    }
}
