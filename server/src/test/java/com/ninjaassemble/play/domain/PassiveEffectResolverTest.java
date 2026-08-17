package com.ninjaassemble.play.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.battle.sim.PassiveTrigger;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class PassiveEffectResolverTest {
    private final HeroContentCatalogService catalog = new HeroContentCatalogService();
    private final PassiveEffectResolver resolver = new PassiveEffectResolver();

    @Test
    void everyPassiveTechniqueResolvesToExecutableLifecycleEffects() {
        EnumSet<PassiveTrigger> observed = EnumSet.noneOf(PassiveTrigger.class);
        int passiveCount = 0;
        for (HeroContentCatalogService.TechniqueView technique : catalog.allTechniques()) {
            if (!"PASSIVE".equals(technique.kind())) continue;
            var passive = resolver.resolve(technique);
            assertEquals(technique.id(), passive.id());
            assertFalse(passive.effects().isEmpty());
            assertTrue(passive.effects().stream().allMatch(effect -> effect.durationTurns() == 0));
            observed.add(passive.trigger());
            passiveCount++;
        }
        assertTrue(passiveCount > 0);
        assertEquals(EnumSet.allOf(PassiveTrigger.class), observed);
    }

    @Test
    void signaturePassivesKeepDistinctTriggerSemanticsAndRealtimeDurations() {
        var jinchuriki = resolver.resolve(catalog.technique("passive-jinchuriki"));
        assertEquals(PassiveTrigger.SELF_LOW_HP, jinchuriki.trigger());
        assertEquals(9_000L, jinchuriki.effects().get(1).durationMs());

        assertEquals(PassiveTrigger.TURN_START, resolver.resolve(catalog.technique("passive-medical")).trigger());
        assertEquals(PassiveTrigger.AFTER_DAMAGE_TAKEN, resolver.resolve(catalog.technique("passive-sharingan")).trigger());

        var swordsman = resolver.resolve(catalog.technique("passive-swordsman"));
        assertEquals(PassiveTrigger.AFTER_DAMAGE_DEALT, swordsman.trigger());
        assertEquals(6_000L, swordsman.effects().get(0).durationMs());

        assertEquals(PassiveTrigger.ALLY_KO, resolver.resolve(catalog.technique("passive-will-of-fire")).trigger());
        assertEquals(PassiveTrigger.BATTLE_START, resolver.resolve(catalog.technique("passive-rinnegan")).trigger());
    }
}
