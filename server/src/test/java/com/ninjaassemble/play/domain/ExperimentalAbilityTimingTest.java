package com.ninjaassemble.play.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import org.junit.jupiter.api.Test;

class ExperimentalAbilityTimingTest {
    @Test
    void productionAbilityCycleUsesExplicitRealtimeTimingForEverySlot() {
        ExperimentalCombatStatsResolver resolver = new ExperimentalCombatStatsResolver(
                new HeroContentCatalogService(), new ExperimentalAbilityProfile(), new PassiveEffectResolver());
        var abilities = resolver.resolve("timing-unit", "naruto-sage", false, 20, TeamSide.A, 0).abilities();

        assertEquals(0L, abilities.basic().cooldownMs());
        assertEquals(0L, abilities.basic().castTimeMs());
        assertEquals(150L, abilities.basic().recoveryMs());

        assertEquals(5_000L, abilities.skill1().cooldownMs());
        assertEquals(300L, abilities.skill1().castTimeMs());
        assertEquals(250L, abilities.skill1().recoveryMs());

        assertEquals(7_000L, abilities.skill2().cooldownMs());
        assertEquals(300L, abilities.skill2().castTimeMs());
        assertEquals(250L, abilities.skill2().recoveryMs());

        assertEquals(10_000L, abilities.ultimate().cooldownMs());
        assertEquals(550L, abilities.ultimate().castTimeMs());
        assertEquals(400L, abilities.ultimate().recoveryMs());
    }
}
