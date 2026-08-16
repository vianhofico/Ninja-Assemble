package com.ninjaassemble.hero.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HeroContentCatalogServiceTest {
    @Test
    void packagedContentHasFullVariantAndKitCoverage() {
        VariantCatalogService variants = new VariantCatalogService();
        HeroContentCatalogService content = new HeroContentCatalogService();
        assertEquals(427, variants.all().size());
        assertEquals(120, content.techniqueCount());
        assertEquals(44, content.profileCount());
        assertEquals(189, content.mappedCharacterCount());
    }

    @Test
    void flagshipFormUsesVariantSpecificKit() {
        HeroContentCatalogService content = new HeroContentCatalogService();
        var base = content.resolve("naruto-uzumaki", null);
        var sage = content.resolve("naruto-uzumaki", "Sage Mode");
        assertEquals("naruto", base.profileId());
        assertEquals("naruto-sage", sage.profileId());
        assertEquals(5, sage.techniques().size());
        assertTrue(sage.techniques().stream().allMatch(t -> !t.nameVi().isBlank() && !t.descriptionVi().isBlank()));
    }
}
