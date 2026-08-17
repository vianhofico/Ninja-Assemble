package com.ninjaassemble.battle.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.List;
import org.junit.jupiter.api.Test;

class RealtimeDeterministicBattleEngineTest {
    private final RealtimeDeterministicBattleEngine engine = new RealtimeDeterministicBattleEngine();
    private final RealtimeBattleRuleset rules = RealtimeBattleRuleset.experimentalV1();

    @Test
    void sameSeedProducesExactlySameTimestampedReplay() {
        BattleRequest request = new BattleRequest(42L, BattleRuleset.experimentalV1(), List.of(
                unit("naruto", TeamSide.A, 0, 10_000, 120, 80, 20, 20, 130),
                unit("sasuke", TeamSide.B, 0, 10_000, 90, 130, 20, 20, 115)
        ));

        RealtimeBattleResult one = engine.simulate(request, rules);
        RealtimeBattleResult two = engine.simulate(request, rules);

        assertEquals(one, two);
        assertFalse(one.events().isEmpty());
        assertTrue(one.events().stream().allMatch(it -> it.timestampMs() % rules.simulationTickMs() == 0));
    }

    @Test
    void combatantsAdvanceOnIndependentSpeedBasedTimelines() {
        BattleRequest request = new BattleRequest(7L, BattleRuleset.experimentalV1(), List.of(
                unit("fast", TeamSide.A, 0, 100_000, 10, 10, 0, 0, 200),
                unit("normal", TeamSide.B, 0, 100_000, 10, 10, 0, 0, 100)
        ));

        List<RealtimeBattleEvent> ready = engine.simulate(request, rules).events().stream()
                .filter(it -> it.type() == BattleEventType.ACTION_READY)
                .limit(2)
                .toList();

        assertEquals(2, ready.size());
        assertEquals("fast", ready.get(0).actorId());
        assertEquals(1_500L, ready.get(0).timestampMs());
        assertEquals("normal", ready.get(1).actorId());
        assertEquals(3_000L, ready.get(1).timestampMs());
    }

    @Test
    void equalTimestampUsesStableTeamSlotActorOrdering() {
        BattleRequest request = new BattleRequest(9L, BattleRuleset.experimentalV1(), List.of(
                unit("z-team-b", TeamSide.B, 0, 100_000, 1, 1, 0, 0, 100),
                unit("a-team-a", TeamSide.A, 0, 100_000, 1, 1, 0, 0, 100)
        ));

        List<RealtimeBattleEvent> ready = engine.simulate(request, rules).events().stream()
                .filter(it -> it.type() == BattleEventType.ACTION_READY && it.timestampMs() == 3_000L)
                .toList();

        assertEquals(List.of("a-team-a", "z-team-b"), ready.stream().map(RealtimeBattleEvent::actorId).toList());
    }

    @Test
    void realtimeStatusTicksAndExpiresByMilliseconds() {
        SkillEffectDefinition burn = new SkillEffectDefinition(
                EffectType.STATUS,
                TargetSelector.FRONTMOST_ENEMY,
                DamageChannel.PHYSICAL,
                1_000,
                0,
                "BURN",
                10_000,
                0,
                3_000L,
                1_000L
        );
        BattleAbility burnAbility = new BattleAbility(
                "burn-test",
                BattleAbilityKind.BASIC,
                DamageChannel.PHYSICAL,
                1_000,
                0,
                "vfx/techniques/burn-test",
                List.of(burn),
                0L,
                0L,
                0L
        );
        BattleAbilitySet burnSet = new BattleAbilitySet(burnAbility, burnAbility, burnAbility, burnAbility);
        BattleUnitSeed attacker = new BattleUnitSeed(
                "attacker", TeamSide.A, 0, 100_000, 100, 0, 0, 0, 100, 0, 0,
                DamageChannel.PHYSICAL, burnSet);
        BattleUnitSeed defender = unit("defender", TeamSide.B, 0, 100_000, 1, 1, 0, 0, 10);

        RealtimeBattleResult result = engine.simulate(
                new BattleRequest(11L, BattleRuleset.experimentalV1(), List.of(attacker, defender)), rules);

        List<RealtimeBattleEvent> ticks = result.events().stream()
                .filter(it -> it.type() == BattleEventType.STATUS_TICK && "BURN".equals(it.statusId()))
                .limit(3)
                .toList();
        RealtimeBattleEvent expired = result.events().stream()
                .filter(it -> it.type() == BattleEventType.STATUS_EXPIRED && "BURN".equals(it.statusId()))
                .findFirst()
                .orElseThrow();

        assertEquals(List.of(4_000L, 5_000L, 6_000L), ticks.stream().map(RealtimeBattleEvent::timestampMs).toList());
        assertEquals(6_000L, expired.timestampMs());
    }

    private static BattleUnitSeed unit(
            String id,
            TeamSide side,
            int slot,
            long hp,
            long physicalAttack,
            long chakraAttack,
            long physicalDefense,
            long chakraDefense,
            int speed
    ) {
        return new BattleUnitSeed(
                id,
                side,
                slot,
                hp,
                physicalAttack,
                chakraAttack,
                physicalDefense,
                chakraDefense,
                speed,
                0,
                0,
                DamageChannel.PHYSICAL
        );
    }
}
