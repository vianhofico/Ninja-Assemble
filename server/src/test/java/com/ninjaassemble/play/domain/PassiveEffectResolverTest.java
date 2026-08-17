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
    void everyPassiveTechniqueResolvesToExecutableEventOrTimeLifecycleEffects() {
        EnumSet<PassiveTrigger> observed = EnumSet.noneOf(PassiveTrigger.class);
        int passiveCount = 0;
        for (HeroContentCatalogService.TechniqueView technique : catalog.allTechniques()) {
            if (!"PASSIVE".equals(technique.kind())) continue;
            var passive = resolver.resolve(technique);
            assertEquals(technique.id(), passive.id());
            assertFalse(passive.effects().isEmpty());
            assertTrue(passive.trigger() != null);
            if (passive.trigger() == PassiveTrigger.TIME_INTERVAL) assertTrue(passive.intervalMs() > 0);
            observed.add(passive.trigger());
            passiveCount++;
        }
        assertTrue(passiveCount > 0);
        assertTrue(observed.contains(PassiveTrigger.BATTLE_START));
        assertTrue(observed.contains(PassiveTrigger.TIME_INTERVAL));
        assertTrue(observed.contains(PassiveTrigger.AFTER_DAMAGE_TAKEN));
        assertTrue(observed.contains(PassiveTrigger.AFTER_DAMAGE_DEALT));
        assertTrue(observed.contains(PassiveTrigger.ALLY_KO));
        assertTrue(observed.contains(PassiveTrigger.HP_THRESHOLD));
    }

    @Test
    void signaturePassivesUseRealTimeOrEventSemantics() {
        assertEquals(PassiveTrigger.HP_THRESHOLD, resolver.resolve(catalog.technique("passive-jinchuriki")).trigger());
        var medical = resolver.resolve(catalog.technique("passive-medical"));
        assertEquals(PassiveTrigger.TIME_INTERVAL, medical.trigger());
        assertEquals(3_000L, medical.intervalMs());
        assertEquals(PassiveTrigger.AFTER_DAMAGE_TAKEN, resolver.resolve(catalog.technique("passive-sharingan")).trigger());
        assertEquals(PassiveTrigger.AFTER_DAMAGE_DEALT, resolver.resolve(catalog.technique("passive-swordsman")).trigger());
        assertEquals(PassiveTrigger.ALLY_KO, resolver.resolve(catalog.technique("passive-will-of-fire")).trigger());
        assertEquals(PassiveTrigger.BATTLE_START, resolver.resolve(catalog.technique("passive-rinnegan")).trigger());
    }
}
