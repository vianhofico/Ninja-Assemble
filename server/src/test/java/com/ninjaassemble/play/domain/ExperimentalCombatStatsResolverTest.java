package com.ninjaassemble.play.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import org.junit.jupiter.api.Test;

class ExperimentalCombatStatsResolverTest {
    @Test
    void sameHeroVersionStateAndLevelAlwaysResolveTheSameStatsAbilitiesAndPassive() {
        ExperimentalCombatStatsResolver resolver = new ExperimentalCombatStatsResolver(
                new HeroContentCatalogService(), new ExperimentalAbilityProfile(), new PassiveEffectResolver());
        var first = resolver.resolve("unit-a", "naruto-sage", false, 20, TeamSide.A, 0);
        var second = resolver.resolve("unit-b", "naruto-sage", false, 20, TeamSide.A, 0);
        assertEquals(first.maxHp(), second.maxHp());
        assertEquals(first.physicalAttack(), second.physicalAttack());
        assertEquals(first.chakraAttack(), second.chakraAttack());
        assertEquals(first.speed(), second.speed());
        assertEquals(first.primaryChannel(), second.primaryChannel());
        assertEquals(first.abilities(), second.abilities());
        assertEquals(first.passives(), second.passives());
        assertFalse(first.passives().isEmpty());
    }

    @Test
    void awakenedStateIsPartOfCombatIdentityEvenBeforeSixthSkillExecutionIsEnabled() {
        ExperimentalCombatStatsResolver resolver = new ExperimentalCombatStatsResolver(
                new HeroContentCatalogService(), new ExperimentalAbilityProfile(), new PassiveEffectResolver());
        var normal = resolver.resolve("unit-a", "naruto-sage", false, 20, TeamSide.A, 0);
        var awakened = resolver.resolve("unit-b", "naruto-sage", true, 20, TeamSide.A, 0);
        assertNotEquals(normal.maxHp(), awakened.maxHp());
    }
}
