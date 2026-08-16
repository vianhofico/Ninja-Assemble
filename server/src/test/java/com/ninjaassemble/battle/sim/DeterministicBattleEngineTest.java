package com.ninjaassemble.battle.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.ninjaassemble.battle.domain.DamageChannel;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicBattleEngineTest {
    @Test
    void sameSeedAndRulesetProduceSameTimeline() {
        BattleRequest request = new BattleRequest(42L, BattleRuleset.experimentalV1(), List.of(
                unit("naruto", TeamSide.A, 0, 100, 30, 20, 5, 5, 20, 3000, DamageChannel.PHYSICAL),
                unit("sasuke", TeamSide.B, 0, 100, 20, 32, 5, 5, 18, 3000, DamageChannel.CHAKRA)
        ));
        DeterministicBattleEngine engine = new DeterministicBattleEngine();
        BattleResult one = engine.simulate(request);
        BattleResult two = engine.simulate(request);
        assertEquals(one, two);
        assertFalse(one.events().isEmpty());
    }

    @Test
    void frontmostLivingEnemyIsTargetedFirst() {
        BattleRequest request = new BattleRequest(1L, BattleRuleset.experimentalV1(), List.of(
                unit("attacker", TeamSide.A, 0, 100, 100, 0, 0, 0, 50, 0, DamageChannel.PHYSICAL),
                unit("front", TeamSide.B, 0, 20, 1, 0, 0, 0, 10, 0, DamageChannel.PHYSICAL),
                unit("rear", TeamSide.B, 1, 100, 1, 0, 0, 0, 9, 0, DamageChannel.PHYSICAL)
        ));
        BattleResult result = new DeterministicBattleEngine().simulate(request);
        BattleEvent firstDamage = result.events().stream().filter(it -> it.type() == BattleEventType.DAMAGE).findFirst().orElseThrow();
        assertEquals("front", firstDamage.targetId());
    }

    @Test
    void executableAbilityCycleBuildsEnergyThenUsesUltimate() {
        BattleAbilitySet set = new BattleAbilitySet(
                ability("basic-kunai", BattleAbilityKind.BASIC, 30),
                ability("fireball-jutsu", BattleAbilityKind.SKILL1, 35),
                ability("chidori", BattleAbilityKind.SKILL2, 35),
                ability("kirin", BattleAbilityKind.ULTIMATE, -100));
        BattleUnitSeed attacker = new BattleUnitSeed("attacker", TeamSide.A, 0, 100_000, 20, 20, 0, 0, 100, 0, 0, DamageChannel.PHYSICAL, set);
        BattleUnitSeed defender = new BattleUnitSeed("defender", TeamSide.B, 0, 100_000, 1, 1, 0, 0, 1, 0, 0, DamageChannel.PHYSICAL);

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(7L, BattleRuleset.experimentalV1(), List.of(attacker, defender)));
        List<BattleEvent> attacks = result.events().stream()
                .filter(it -> it.type() == BattleEventType.ATTACK && "attacker".equals(it.actorId()))
                .limit(4)
                .toList();

        assertEquals(List.of(BattleAbilityKind.BASIC, BattleAbilityKind.SKILL1, BattleAbilityKind.SKILL2, BattleAbilityKind.ULTIMATE),
                attacks.stream().map(BattleEvent::abilityKind).toList());
        assertEquals(List.of(30, 65, 100, 0), attacks.stream().map(BattleEvent::energyAfter).toList());
        assertEquals("kirin", attacks.get(3).abilityId());
        assertEquals("vfx/techniques/kirin", attacks.get(3).effectKey());
    }

    private static BattleAbility ability(String id, BattleAbilityKind kind, int energyDelta) {
        return new BattleAbility(id, kind, DamageChannel.PHYSICAL, 1_000, energyDelta, "vfx/techniques/" + id);
    }

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long patk, long catk, long pdef, long cdef, int speed, int crit, DamageChannel channel) {
        return new BattleUnitSeed(id, side, slot, hp, patk, catk, pdef, cdef, speed, crit, crit, channel);
    }
}
