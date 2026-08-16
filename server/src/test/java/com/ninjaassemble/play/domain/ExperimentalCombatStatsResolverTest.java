package com.ninjaassemble.play.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import org.junit.jupiter.api.Test;

class ExperimentalCombatStatsResolverTest {
    @Test
    void sameCharacterVariantAndLevelAlwaysResolveTheSameStats() {
        ExperimentalCombatStatsResolver resolver = new ExperimentalCombatStatsResolver(new HeroContentCatalogService());
        var one = resolver.resolve("unit-a", "naruto-uzumaki", "Sage Mode", 20, TeamSide.A, 0);
        var two = resolver.resolve("unit-b", "naruto-uzumaki", "Sage Mode", 20, TeamSide.A, 0);
        assertEquals(one.maxHp(), two.maxHp());
        assertEquals(one.physicalAttack(), two.physicalAttack());
        assertEquals(one.chakraAttack(), two.chakraAttack());
        assertEquals(one.speed(), two.speed());
        assertEquals(one.primaryChannel(), two.primaryChannel());
    }
}
