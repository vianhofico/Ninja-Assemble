package com.ninjaassemble.battle.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.battle.domain.DamageChannel;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeterministicBattleEngineTest {
    @Test
    void sameSeedProducesIdenticalTimestampedTimeline() {
        BattleRequest request = new BattleRequest(77L, BattleRuleset.experimentalV1(), List.of(
                unit("a", TeamSide.A, 0, 20_000, 300, 1_200),
                unit("b", TeamSide.B, 0, 20_000, 300, 1_000)));
        DeterministicBattleEngine engine = new DeterministicBattleEngine();
        BattleResult first = engine.simulate(request);
        BattleResult second = engine.simulate(request);
        assertEquals(first.outcome(), second.outcome());
        assertEquals(first.durationMs(), second.durationMs());
        assertEquals(first.events(), second.events());
        assertEquals(first.finalHp(), second.finalHp());
        assertTrue(first.events().stream().allMatch(it -> it.timestampMs() >= 0));
    }

    @Test
    void speedChangesIndependentActionFrequency() {
        BattleRuleset shortFight = new BattleRuleset("speed-test", 0, 1_000, 0, 8_000,
                1_000, 1_500, 400, 3_000, 15, 100, 50);
        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(88L, shortFight, List.of(
                unit("fast", TeamSide.A, 0, 1_000_000, 1, 1_500),
                unit("slow", TeamSide.A, 1, 1_000_000, 1, 750),
                unit("dummy", TeamSide.B, 0, 1_000_000, 1, 1_000))));
        long fast = basicStarts(result, "fast");
        long slow = basicStarts(result, "slow");
        assertTrue(fast > slow, "fast actor must attack more frequently: fast=" + fast + " slow=" + slow);
        assertFalse(result.events().stream().anyMatch(it -> it.type().name().contains("ROUND") || it.type().name().contains("TURN")));
    }

    @Test
    void rageCapsAtOneHundredAndUnlocksSignatureRageSkill() {
        BattleRuleset rules = new BattleRuleset("rage-test", 0, 1_000, 0, 20_000,
                1_000, 800, 300, 2_000, 20, 100, 50);
        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(99L, rules, List.of(
                unit("hero", TeamSide.A, 0, 1_000_000, 1, 1_000),
                unit("dummy", TeamSide.B, 0, 1_000_000, 1, 1_000))));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.RAGE_FULL && "hero".equals(it.actorId())));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.RAGE_SKILL_CAST_START && "hero".equals(it.actorId())));
        assertTrue(result.events().stream().allMatch(it -> it.rageAfter() >= 0 && it.rageAfter() <= 100));
    }

    @Test
    void equalTimestampActionsUseStableActorOrdering() {
        BattleRuleset rules = new BattleRuleset("tie-test", 0, 1_000, 0, 2_000,
                1_000, 1_000, 400, 2_000, 15, 100, 50);
        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(100L, rules, List.of(
                unit("alpha", TeamSide.A, 0, 100_000, 1, 1_000),
                unit("beta", TeamSide.B, 0, 100_000, 1, 1_000))));
        List<BattleEvent> ready = result.events().stream().filter(it -> it.type() == BattleEventType.ACTION_READY && it.timestampMs() == 1_000).toList();
        assertEquals(2, ready.size());
        assertEquals("alpha", ready.get(0).actorId());
        assertEquals("beta", ready.get(1).actorId());
    }

    private static long basicStarts(BattleResult result, String actor) {
        return result.events().stream().filter(it -> it.type() == BattleEventType.BASIC_ATTACK_START && actor.equals(it.actorId())).count();
    }

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long attack, int speed) {
        return new BattleUnitSeed(id, side, slot, hp, attack, attack, 0, 0, speed, 0, 0,
                DamageChannel.PHYSICAL, BattleAbilitySet.basicOnly(DamageChannel.PHYSICAL));
    }
}
