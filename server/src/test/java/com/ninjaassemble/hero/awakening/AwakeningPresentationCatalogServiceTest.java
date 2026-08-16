package com.ninjaassemble.hero.awakening;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.play.domain.BattleParticipant;
import org.junit.jupiter.api.Test;

class AwakeningPresentationCatalogServiceTest {
    @Test
    void everyCatalogEntryUsesExplicitAwakeningAndHeroIdentity() {
        AwakeningPresentationCatalogService catalog = new AwakeningPresentationCatalogService();
        assertTrue(catalog.size() > 0);
        var naruto = catalog.findForHero("naruto-sage");
        assertEquals("awakening-naruto-sage", naruto.awakeningId());
        assertEquals("naruto-sage", naruto.heroId());
        assertFalse(naruto.awakeningSkillVfx().isBlank());
        assertFalse(naruto.cameraSequence().isBlank());
        assertEquals("ASSET_SPEC_PENDING_PRODUCTION", naruto.status());
    }

    @Test
    void battleParticipantCarriesOneAwakeningPresentationIdentity() {
        BattleParticipant normal = BattleParticipant.heroVersion(
                "unit-normal", "naruto-uzumaki", "naruto-sage", false, "",
                "Naruto Sage", 80, TeamSide.A, 0, 1_000);
        assertEquals("hero/naruto-sage/base", normal.presentationKey());
        assertFalse(normal.awakened());

        BattleParticipant awakened = BattleParticipant.heroVersion(
                "unit-awakened", "naruto-uzumaki", "naruto-sage", true, "awakening-naruto-sage",
                "Naruto Sage", 80, TeamSide.A, 0, 1_000);
        assertEquals("hero/naruto-sage/awakened", awakened.presentationKey());
        assertTrue(awakened.awakened());
        assertEquals("awakening-naruto-sage", awakened.awakeningId());

        assertThrows(IllegalArgumentException.class, () -> BattleParticipant.heroVersion(
                "invalid", "naruto-uzumaki", "naruto-sage", true, "", "Naruto", 80, TeamSide.A, 0, 1_000));
    }
}
