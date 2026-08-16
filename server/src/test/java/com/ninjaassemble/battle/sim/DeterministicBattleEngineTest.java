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

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long patk, long catk, long pdef, long cdef, int speed, int crit, DamageChannel channel) {
        return new BattleUnitSeed(id, side, slot, hp, patk, catk, pdef, cdef, speed, crit, crit, channel);
    }
}
