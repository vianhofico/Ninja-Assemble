package com.ninjaassemble.hero.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void curatedM47OverridesDifferentiateSameCharacterVersionsAtRuntime() {
        HeroContentCatalogService content = new HeroContentCatalogService();

        var kakashiYoung = content.resolveHero("kakashi-young", false);
        var kakashiWar = content.resolveHero("kakashi-war", false);
        assertEquals("chidori", kakashiYoung.skill("ULTIMATE").sourceTechniqueId());
        assertEquals("kamui", kakashiWar.skill("ULTIMATE").sourceTechniqueId());
        assertNotEquals(kakashiYoung.skill("ULTIMATE").sourceTechniqueId(), kakashiWar.skill("ULTIMATE").sourceTechniqueId());

        var narutoHokage = content.resolveHero("naruto-hokage", false);
        var narutoSixPaths = content.resolveHero("naruto-six-paths", false);
        assertEquals("tailed-beast-bomb", narutoHokage.skill("ULTIMATE").sourceTechniqueId());
        assertEquals("six-paths-barrage", narutoSixPaths.skill("ULTIMATE").sourceTechniqueId());

        var obitoYoung = content.resolveHero("obito-young", false);
        var obitoMasked = content.resolveHero("obito-masked", false);
        var obitoWhiteMask = content.resolveHero("obito-white-mask", false);
        assertEquals("taijutsu-combo", obitoYoung.skill("SKILL_1").sourceTechniqueId());
        assertEquals("kamui-phase", obitoMasked.skill("SKILL_1").sourceTechniqueId());
        assertEquals("passive-rinnegan", obitoWhiteMask.skill("PASSIVE").sourceTechniqueId());
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
