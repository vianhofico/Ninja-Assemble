package com.ninjaassemble.battle.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.List;
import org.junit.jupiter.api.Test;

class PassiveLifecycleBattleTest {
    @Test
    void battleStartPassiveTriggersAtLogicalTimeZeroAndCanGrantRage() {
        BattlePassive passive = new BattlePassive("start-rage", PassiveTrigger.BATTLE_START,
                List.of(instant(EffectType.RAGE, TargetSelector.SELF, null, 0, 30, null)), true, 0);
        BattleUnitSeed owner = unit("owner", TeamSide.A, 0, 10_000, 20, 1_000, List.of(passive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 10_000, 20, 1_000, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(101L, rules(4_000), List.of(owner, enemy)));
        BattleEvent trigger = result.events().stream().filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER).findFirst().orElseThrow();
        BattleEvent rage = result.events().stream().filter(it -> it.type() == BattleEventType.RAGE_GAIN && "start-rage".equals(it.abilityId())).findFirst().orElseThrow();
        assertEquals(0L, trigger.timestampMs());
        assertEquals("BATTLE_START", trigger.triggerId());
        assertEquals(30, rage.rageAfter());
    }

    @Test
    void periodicMedicalPassiveUsesThreeSecondIntervalNotTurnStart() {
        BattlePassive passive = new BattlePassive("periodic-heal", PassiveTrigger.TIME_INTERVAL,
                List.of(instant(EffectType.HEAL, TargetSelector.LOWEST_HP_ALLY, DamageChannel.CHAKRA, 0, 25, null)), false, 0, 3_000);
        BattleUnitSeed owner = unit("owner", TeamSide.A, 0, 10_000, 1, 700, List.of(passive));
        BattleUnitSeed ally = unit("ally", TeamSide.A, 1, 500, 1, 700, List.of());
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 20_000, 50, 2_000, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(102L, rules(7_000), List.of(owner, ally, enemy)));
        List<Long> triggers = result.events().stream().filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "periodic-heal".equals(it.abilityId()))
                .map(BattleEvent::timestampMs).toList();
        assertTrue(triggers.contains(3_000L));
        assertTrue(triggers.contains(6_000L));
    }

    @Test
    void afterDamageTakenPassiveReactsWithoutRecursivePassiveChains() {
        BattlePassive passive = new BattlePassive("react-rage", PassiveTrigger.AFTER_DAMAGE_TAKEN,
                List.of(instant(EffectType.RAGE, TargetSelector.SELF, null, 0, 10, null)), false, 0);
        BattleUnitSeed defender = unit("defender", TeamSide.A, 0, 10_000, 10, 900, List.of(passive));
        BattleUnitSeed attacker = unit("attacker", TeamSide.B, 0, 10_000, 60, 1_200, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(103L, rules(5_000), List.of(defender, attacker)));
        BattleEvent trigger = result.events().stream().filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "react-rage".equals(it.abilityId())).findFirst().orElseThrow();
        BattleEvent rage = result.events().stream().filter(it -> it.type() == BattleEventType.RAGE_GAIN && "react-rage".equals(it.abilityId())).findFirst().orElseThrow();
        assertEquals("AFTER_DAMAGE_TAKEN", trigger.triggerId());
        assertEquals(10, rage.amount());
    }

    @Test
    void hpThresholdPassiveFiresOnlyOncePerBattle() {
        BattlePassive passive = new BattlePassive("low-heal", PassiveTrigger.HP_THRESHOLD,
                List.of(instant(EffectType.HEAL, TargetSelector.SELF, DamageChannel.PHYSICAL, 5_000, 0, null)), true, 4_000);
        BattleUnitSeed owner = unit("owner", TeamSide.A, 0, 100, 20, 700, List.of(passive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 10_000, 70, 1_500, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(104L, rules(8_000), List.of(owner, enemy)));
        long triggers = result.events().stream().filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "low-heal".equals(it.abilityId())).count();
        assertEquals(1L, triggers);
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.HEAL && "low-heal".equals(it.abilityId())));
    }

    @Test
    void allyKoPassiveTriggersForLivingTeammate() {
        BattlePassive passive = new BattlePassive("ally-ko-buff", PassiveTrigger.ALLY_KO,
                List.of(new SkillEffectDefinition(EffectType.STATUS, TargetSelector.SELF, DamageChannel.PHYSICAL, 1_000, 0, "ATK_UP", 10_000, 5_000, 0)), false, 0);
        BattleUnitSeed victim = unit("victim", TeamSide.A, 0, 20, 1, 500, List.of());
        BattleUnitSeed survivor = unit("survivor", TeamSide.A, 1, 10_000, 20, 700, List.of(passive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 10_000, 100, 2_000, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(105L, rules(6_000), List.of(victim, survivor, enemy)));
        BattleEvent trigger = result.events().stream().filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "ally-ko-buff".equals(it.abilityId())).findFirst().orElseThrow();
        assertEquals("survivor", trigger.actorId());
        assertEquals("ALLY_KO", trigger.triggerId());
    }

    private static BattleRuleset rules(long duration) {
        return new BattleRuleset("passive-test", 0, 1_000, 0, duration, 1_000, 1_000, 300, 3_000, 15, 100, 50);
    }

    private static SkillEffectDefinition instant(EffectType type, TargetSelector target, DamageChannel channel,
                                                 int coefficientBps, long flatAmount, String status) {
        return new SkillEffectDefinition(type, target, channel, coefficientBps, flatAmount, status, 10_000);
    }

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long attack, int speed, List<BattlePassive> passives) {
        return new BattleUnitSeed(id, side, slot, hp, attack, attack, 0, 0, speed, 0, 0,
                DamageChannel.PHYSICAL, BattleAbilitySet.basicOnly(DamageChannel.PHYSICAL), passives);
    }
}
