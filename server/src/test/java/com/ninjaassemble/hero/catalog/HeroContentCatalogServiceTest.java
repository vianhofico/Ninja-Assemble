package com.ninjaassemble.hero.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class HeroContentCatalogServiceTest {
    @Test
    void packagedRuntimeCatalogUsesOnlyExplicitHeroVersionData() {
        HeroContentCatalogService content = new HeroContentCatalogService();
        assertEquals(120, content.techniqueCount());
        assertEquals(194, content.heroCount());
        assertEquals(970, content.heroSkillCount());
        assertEquals(60, content.awakeningSkillCount());
        assertEquals(194, content.profileCount());
        assertTrue(content.mappedCharacterCount() > 150);
    }

    @Test
    void normalHeroHasFiveSlotsAndAwakenedHeroHasUniqueSixthSlot() {
        HeroContentCatalogService content = new HeroContentCatalogService();
        var normal = content.resolveHero("naruto-sage", false);
        var awakened = content.resolveHero("naruto-sage", true);

        assertEquals("naruto-sage", normal.heroId());
        assertEquals(5, normal.skills().size());
        assertEquals(6, awakened.skills().size());
        assertEquals("AWAKENING_SKILL", awakened.skills().get(5).slot());
        assertEquals("awaken-skill-naruto-sage", awakened.skills().get(5).skillId());
        assertFalse(awakened.skills().get(5).executable(), "M47 must explicitly design the sixth skill before execution");
        assertTrue(normal.techniques().stream().allMatch(t -> !t.nameVi().isBlank() && !t.descriptionVi().isBlank()));
    }

    @Test
    void legacyVariantCanOnlyResolveThroughAuditedBridge() {
        HeroContentCatalogService content = new HeroContentCatalogService();
        var kcm2 = content.resolveLegacyIdentity("naruto-uzumaki", "KCM2");
        assertEquals("naruto-sage", kcm2.heroId());
        assertTrue(kcm2.awakened());
        assertThrows(IllegalArgumentException.class, () -> content.resolve("aoda", "Great Snake"));
    }
}
