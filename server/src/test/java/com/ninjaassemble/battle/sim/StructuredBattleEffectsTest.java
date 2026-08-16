package com.ninjaassemble.battle.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructuredBattleEffectsTest {
    @Test
    void areaDamageHitsEveryLivingEnemyAndShieldAbsorbsLaterDamage() {
        BattleAbility areaAndShield = ability("area-guard", List.of(
                effect(EffectType.SHIELD, TargetSelector.SELF, null, 0, 50, null, 10_000, 0),
                effect(EffectType.DAMAGE, TargetSelector.ALL_ENEMIES, DamageChannel.PHYSICAL, 10_000, 0, null, 10_000, 0)));
        BattleUnitSeed actor = unit("actor", TeamSide.A, 0, 1_000, 100, 100, 0, 0, 100, set(areaAndShield));
        BattleUnitSeed enemyFront = unit("enemy-front", TeamSide.B, 0, 1_000, 100, 100, 0, 0, 50, null);
        BattleUnitSeed enemyRear = unit("enemy-rear", TeamSide.B, 1, 1_000, 100, 100, 0, 0, 40, null);

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(11L, BattleRuleset.experimentalV1(), List.of(actor, enemyFront, enemyRear)));
        List<BattleEvent> roundOneAreaDamage = result.events().stream()
                .filter(it -> it.round() == 1 && it.type() == BattleEventType.DAMAGE && "actor".equals(it.actorId()))
                .toList();
        assertEquals(List.of("enemy-front", "enemy-rear"), roundOneAreaDamage.stream().map(BattleEvent::targetId).toList());
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.SHIELD && "actor".equals(it.targetId())));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.SHIELD_ABSORB && "actor".equals(it.targetId())));
    }

    @Test
    void stunSkipsTheTargetsNextScheduledTurn() {
        BattleAbility stun = ability("stun-seal", List.of(
                effect(EffectType.STATUS, TargetSelector.FRONTMOST_ENEMY, DamageChannel.CHAKRA, 0, 0, "STUN", 10_000, 1)));
        BattleUnitSeed controller = unit("controller", TeamSide.A, 0, 1_000, 10, 10, 0, 0, 100, set(stun));
        BattleUnitSeed target = unit("target", TeamSide.B, 0, 1_000, 10, 10, 0, 0, 50, null);

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(12L, BattleRuleset.experimentalV1(), List.of(controller, target)));
        BattleEvent skipped = result.events().stream().filter(it -> it.type() == BattleEventType.TURN_SKIPPED).findFirst().orElseThrow();
        assertEquals("target", skipped.actorId());
        assertEquals("STUN", skipped.statusId());
    }

    @Test
    void burnTicksAtTheStartOfTheAffectedUnitsTurn() {
        BattleAbility burn = ability("burn-mark", List.of(
                effect(EffectType.STATUS, TargetSelector.FRONTMOST_ENEMY, DamageChannel.CHAKRA, 0, 10, "BURN", 10_000, 2)));
        BattleUnitSeed caster = unit("caster", TeamSide.A, 0, 1_000, 10, 10, 0, 0, 100, set(burn));
        BattleUnitSeed target = unit("target", TeamSide.B, 0, 1_000, 10, 10, 0, 0, 50, null);

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(13L, BattleRuleset.experimentalV1(), List.of(caster, target)));
        BattleEvent tick = result.events().stream().filter(it -> it.type() == BattleEventType.STATUS_TICK).findFirst().orElseThrow();
        assertEquals("target", tick.targetId());
        assertEquals(10L, tick.amount());
        assertEquals("BURN", tick.statusId());
    }

    @Test
    void healerCanRestoreDamagedAllyAndReviveAKnockedOutAlly() {
        BattleAbility healAndRevive = ability("restore-team", List.of(
                effect(EffectType.HEAL, TargetSelector.LOWEST_HP_ALLY, DamageChannel.CHAKRA, 0, 40, null, 10_000, 0),
                effect(EffectType.REVIVE, TargetSelector.LOWEST_HP_ALLY, DamageChannel.CHAKRA, 3_000, 30, null, 10_000, 0)));
        BattleAbility lethal = ability("lethal", List.of(
                effect(EffectType.DAMAGE, TargetSelector.FRONTMOST_ENEMY, DamageChannel.PHYSICAL, 10_000, 100, null, 10_000, 0)));
        BattleUnitSeed fallen = unit("fallen", TeamSide.A, 0, 50, 1, 1, 0, 0, 10, null);
        BattleUnitSeed healer = unit("healer", TeamSide.A, 1, 500, 10, 10, 0, 0, 50, set(healAndRevive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 500, 100, 1, 0, 0, 100, set(lethal));

        BattleResult result = new DeterministicBattleEngine().simulate(new BattleRequest(14L, BattleRuleset.experimentalV1(), List.of(fallen, healer, enemy)));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.KO && "fallen".equals(it.targetId())));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.REVIVE && "fallen".equals(it.targetId())));
    }

    private static BattleAbility ability(String id, List<SkillEffectDefinition> effects) {
        return new BattleAbility(id, BattleAbilityKind.BASIC, DamageChannel.PHYSICAL, 10_000, 0, "vfx/techniques/" + id, effects);
    }

    private static BattleAbilitySet set(BattleAbility ability) { return new BattleAbilitySet(ability, ability, ability, ability); }

    private static SkillEffectDefinition effect(EffectType type, TargetSelector target, DamageChannel channel, int coefficient,
                                                long flat, String status, int chance, int duration) {
        return new SkillEffectDefinition(type, target, channel, coefficient, flat, status, chance, duration);
    }

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long patk, long catk, long pdef,
                                       long cdef, int speed, BattleAbilitySet abilities) {
        return new BattleUnitSeed(id, side, slot, hp, patk, catk, pdef, cdef, speed, 0, 0, DamageChannel.PHYSICAL,
                abilities == null ? BattleAbilitySet.basicOnly(DamageChannel.PHYSICAL) : abilities);
    }
}
