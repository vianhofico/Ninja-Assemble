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
    void battleStartPassiveTriggersBeforeRoundOneAndCanGrantEnergy() {
        BattlePassive passive = new BattlePassive("start-energy", PassiveTrigger.BATTLE_START,
                List.of(effect(EffectType.ENERGY, TargetSelector.SELF, null, 0, 30, null, 0)), true, 0);
        BattleUnitSeed owner = unit("owner", TeamSide.A, 0, 1_000, 20, 100, List.of(passive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 1_000, 20, 10, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(101L, BattleRuleset.experimentalV1(), List.of(owner, enemy)));
        BattleEvent trigger = result.events().stream().filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER).findFirst().orElseThrow();
        BattleEvent energy = result.events().stream().filter(it -> it.type() == BattleEventType.ENERGY && "start-energy".equals(it.abilityId())).findFirst().orElseThrow();
        BattleEvent roundOne = result.events().stream().filter(it -> it.type() == BattleEventType.ROUND_START).findFirst().orElseThrow();

        assertEquals("BATTLE_START", trigger.triggerId());
        assertEquals(BattleAbilityKind.PASSIVE, trigger.abilityKind());
        assertEquals(30, energy.energyAfter());
        assertTrue(trigger.sequence() < roundOne.sequence());
    }

    @Test
    void afterDamageTakenPassiveReactsWithoutRecursivePassiveChains() {
        BattlePassive passive = new BattlePassive("react-energy", PassiveTrigger.AFTER_DAMAGE_TAKEN,
                List.of(effect(EffectType.ENERGY, TargetSelector.SELF, null, 0, 10, null, 0)), false, 0);
        BattleUnitSeed defender = unit("defender", TeamSide.A, 0, 1_000, 10, 10, List.of(passive));
        BattleUnitSeed attacker = unit("attacker", TeamSide.B, 0, 1_000, 60, 100, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(102L, BattleRuleset.experimentalV1(), List.of(defender, attacker)));
        BattleEvent trigger = result.events().stream()
                .filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "react-energy".equals(it.abilityId()))
                .findFirst().orElseThrow();
        BattleEvent energy = result.events().stream()
                .filter(it -> it.type() == BattleEventType.ENERGY && "react-energy".equals(it.abilityId()))
                .findFirst().orElseThrow();
        assertEquals("AFTER_DAMAGE_TAKEN", trigger.triggerId());
        assertEquals(10, energy.amount());
    }

    @Test
    void lowHpPassiveFiresOnlyOncePerBattle() {
        BattlePassive passive = new BattlePassive("low-heal", PassiveTrigger.SELF_LOW_HP,
                List.of(effect(EffectType.HEAL, TargetSelector.SELF, DamageChannel.PHYSICAL, 5_000, 0, null, 0)), true, 4_000);
        BattleUnitSeed owner = unit("owner", TeamSide.A, 0, 100, 20, 50, List.of(passive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 1_000, 70, 100, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(103L, BattleRuleset.experimentalV1(), List.of(owner, enemy)));
        long triggers = result.events().stream()
                .filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "low-heal".equals(it.abilityId()))
                .count();
        assertEquals(1L, triggers);
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.HEAL && "low-heal".equals(it.abilityId())));
    }

    @Test
    void allyKoPassiveTriggersForLivingTeammate() {
        BattlePassive passive = new BattlePassive("ally-ko-buff", PassiveTrigger.ALLY_KO,
                List.of(effect(EffectType.STATUS, TargetSelector.SELF, DamageChannel.PHYSICAL, 1_000, 0, "ATK_UP", 3)), false, 0);
        BattleUnitSeed victim = unit("victim", TeamSide.A, 0, 20, 1, 10, List.of());
        BattleUnitSeed survivor = unit("survivor", TeamSide.A, 1, 1_000, 20, 20, List.of(passive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 1_000, 100, 100, List.of());

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(104L, BattleRuleset.experimentalV1(), List.of(victim, survivor, enemy)));
        BattleEvent trigger = result.events().stream()
                .filter(it -> it.type() == BattleEventType.PASSIVE_TRIGGER && "ally-ko-buff".equals(it.abilityId()))
                .findFirst().orElseThrow();
        assertEquals("survivor", trigger.actorId());
        assertEquals("ALLY_KO", trigger.triggerId());
    }

    private static SkillEffectDefinition effect(EffectType type, TargetSelector target, DamageChannel channel,
                                                int coefficientBps, long flatAmount, String status, int durationTurns) {
        return new SkillEffectDefinition(type, target, channel, coefficientBps, flatAmount, status, 10_000, durationTurns);
    }

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long attack, int speed, List<BattlePassive> passives) {
        return new BattleUnitSeed(id, side, slot, hp, attack, attack, 0, 0, speed, 0, 0,
                DamageChannel.PHYSICAL, BattleAbilitySet.basicOnly(DamageChannel.PHYSICAL), passives);
    }
}
