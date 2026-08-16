package com.ninjaassemble.progression.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class EvolutionPathCatalogServiceTest {
    @Test
    void flagshipChainsArePackagedAndOrderedByRequirements() {
        EvolutionPathCatalogService catalog = new EvolutionPathCatalogService();
        var naruto = catalog.forCharacter("naruto-uzumaki");
        assertEquals(4, naruto.size());
        assertEquals("Sage Mode", naruto.get(0).targetVariant());
        assertEquals("KCM1", naruto.get(1).targetVariant());
        assertEquals("KCM2", naruto.get(2).targetVariant());
        assertEquals("Six Paths Sage Mode", naruto.get(3).targetVariant());
        assertTrue(catalog.all().size() >= 20);
    }
}
