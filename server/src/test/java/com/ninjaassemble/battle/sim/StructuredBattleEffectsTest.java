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
        BattleAbility areaAndShield = basic("area-guard", List.of(
                instant(EffectType.SHIELD, TargetSelector.SELF, null, 0, 50, null),
                instant(EffectType.DAMAGE, TargetSelector.ALL_ENEMIES, DamageChannel.PHYSICAL, 10_000, 0, null)));
        BattleUnitSeed actor = unit("actor", TeamSide.A, 0, 10_000, 100, 1_000, set(areaAndShield));
        BattleUnitSeed enemyFront = unit("enemy-front", TeamSide.B, 0, 10_000, 100, 900, null);
        BattleUnitSeed enemyRear = unit("enemy-rear", TeamSide.B, 1, 10_000, 100, 800, null);

        BattleResult result = new RealtimeBattleEngine().simulate(new RealtimeBattleRequest(11L, shortRules(5_000), List.of(actor, enemyFront, enemyRear)));
        long firstHitAt = result.events().stream().filter(it -> it.type() == BattleEventType.DAMAGE && "actor".equals(it.actorId())).mapToLong(BattleEvent::timestampMs).min().orElseThrow();
        List<BattleEvent> area = result.events().stream().filter(it -> it.timestampMs() == firstHitAt && it.type() == BattleEventType.DAMAGE && "actor".equals(it.actorId())).toList();
        assertEquals(List.of("enemy-front", "enemy-rear"), area.stream().map(BattleEvent::targetId).toList());
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.SHIELD && "actor".equals(it.targetId())));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.SHIELD_ABSORB && "actor".equals(it.targetId())));
    }

    @Test
    void stunBlocksActionsUntilExactExpirationInsteadOfSkippingATurn() {
        BattlePassive stunAtStart = new BattlePassive("stun-seal", PassiveTrigger.BATTLE_START,
                List.of(timed(EffectType.STATUS, TargetSelector.FRONTMOST_ENEMY, DamageChannel.CHAKRA, 0, 0, "STUN", 2_500, 0)), true, 0);
        BattleUnitSeed controller = unitWithPassives("controller", TeamSide.A, 0, 20_000, 10, 1_000, List.of(stunAtStart));
        BattleUnitSeed target = unit("target", TeamSide.B, 0, 20_000, 10, 1_000, null);

        BattleResult result = new RealtimeBattleEngine().simulate(new RealtimeBattleRequest(12L, shortRules(7_000), List.of(controller, target)));
        BattleEvent applied = result.events().stream().filter(it -> it.type() == BattleEventType.STATUS_APPLIED && "STUN".equals(it.statusId())).findFirst().orElseThrow();
        BattleEvent blocked = result.events().stream().filter(it -> it.type() == BattleEventType.ACTION_BLOCKED && "target".equals(it.actorId())).findFirst().orElseThrow();
        BattleEvent expired = result.events().stream().filter(it -> it.type() == BattleEventType.STATUS_EXPIRED && "STUN".equals(it.statusId())).findFirst().orElseThrow();
        assertEquals(2_500, applied.durationMs());
        assertEquals(0L, applied.timestampMs());
        assertTrue(blocked.timestampMs() >= applied.timestampMs());
        assertEquals(applied.timestampMs() + 2_500, expired.timestampMs());
        assertTrue(result.events().stream().filter(it -> "target".equals(it.actorId()) && it.type() == BattleEventType.BASIC_ATTACK_START)
                .allMatch(it -> it.timestampMs() >= expired.timestampMs()));
    }

    @Test
    void burnTicksOnExplicitOneSecondClockAndTicksAtExpiryBoundary() {
        BattlePassive burnAtStart = new BattlePassive("burn-mark", PassiveTrigger.BATTLE_START,
                List.of(timed(EffectType.STATUS, TargetSelector.FRONTMOST_ENEMY, DamageChannel.CHAKRA, 0, 10, "BURN", 3_000, 1_000)), true, 0);
        BattleUnitSeed caster = unitWithPassives("caster", TeamSide.A, 0, 50_000, 10, 1_000, List.of(burnAtStart));
        BattleUnitSeed target = unit("target", TeamSide.B, 0, 50_000, 10, 500, null);

        BattleResult result = new RealtimeBattleEngine().simulate(new RealtimeBattleRequest(13L, shortRules(5_000), List.of(caster, target)));
        BattleEvent applied = result.events().stream().filter(it -> it.type() == BattleEventType.STATUS_APPLIED && "BURN".equals(it.statusId())).findFirst().orElseThrow();
        List<Long> ticks = result.events().stream().filter(it -> it.type() == BattleEventType.STATUS_TICK && "BURN".equals(it.statusId()))
                .map(BattleEvent::timestampMs).filter(it -> it <= applied.timestampMs() + 3_000).toList();
        assertEquals(0L, applied.timestampMs());
        assertTrue(ticks.contains(1_000L));
        assertTrue(ticks.contains(2_000L));
        assertTrue(ticks.contains(3_000L));
    }

    @Test
    void healerCanRestoreDamagedAllyAndReviveAKnockedOutAlly() {
        BattleAbility healAndRevive = basic("restore-team", List.of(
                instant(EffectType.HEAL, TargetSelector.LOWEST_HP_ALLY, DamageChannel.CHAKRA, 0, 40, null),
                instant(EffectType.REVIVE, TargetSelector.LOWEST_HP_ALLY, DamageChannel.CHAKRA, 3_000, 30, null)));
        BattleAbility lethal = basic("lethal", List.of(
                instant(EffectType.DAMAGE, TargetSelector.FRONTMOST_ENEMY, DamageChannel.PHYSICAL, 10_000, 10_000, null)));
        BattleUnitSeed fallen = unit("fallen", TeamSide.A, 0, 50, 1, 500, null);
        BattleUnitSeed healer = unit("healer", TeamSide.A, 1, 5_000, 10, 1_000, set(healAndRevive));
        BattleUnitSeed enemy = unit("enemy", TeamSide.B, 0, 5_000, 100, 2_000, set(lethal));

        BattleResult result = new RealtimeBattleEngine().simulate(new RealtimeBattleRequest(14L, shortRules(8_000), List.of(fallen, healer, enemy)));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.KO && "fallen".equals(it.targetId())));
        assertTrue(result.events().stream().anyMatch(it -> it.type() == BattleEventType.REVIVE && "fallen".equals(it.targetId())));
    }

    private static BattleRuleset shortRules(long duration) {
        return new BattleRuleset("effects-test", 0, 1_000, 0, duration, 1_000, 1_000, 300, 3_000, 15, 100, 50);
    }

    private static BattleAbility basic(String id, List<SkillEffectDefinition> effects) {
        return new BattleAbility(id, BattleAbilityKind.BASIC, DamageChannel.PHYSICAL, 10_000, 15, "vfx/" + id, effects, 0, 0, 200);
    }

    private static BattleAbilitySet set(BattleAbility basic) {
        BattleAbility s1 = new BattleAbility("s1-" + basic.id(), BattleAbilityKind.SKILL1, basic.channel(), 10_000, 0, basic.effectKey(), basic.effects(), 60_000, 0, 200);
        BattleAbility s2 = new BattleAbility("s2-" + basic.id(), BattleAbilityKind.SKILL2, basic.channel(), 10_000, 0, basic.effectKey(), basic.effects(), 60_000, 0, 200);
        BattleAbility rage = new BattleAbility("rage-" + basic.id(), BattleAbilityKind.RAGE_SKILL, basic.channel(), 10_000, -100, basic.effectKey(), basic.effects(), 0, 0, 200);
        return new BattleAbilitySet(basic, s1, s2, rage);
    }

    private static SkillEffectDefinition instant(EffectType type, TargetSelector target, DamageChannel channel, int coefficient,
                                                 long flat, String status) {
        return new SkillEffectDefinition(type, target, channel, coefficient, flat, status, 10_000);
    }

    private static SkillEffectDefinition timed(EffectType type, TargetSelector target, DamageChannel channel, int coefficient,
                                               long flat, String status, long durationMs, long tickIntervalMs) {
        return new SkillEffectDefinition(type, target, channel, coefficient, flat, status, 10_000, durationMs, tickIntervalMs);
    }

    private static BattleUnitSeed unit(String id, TeamSide side, int slot, long hp, long attack, int speed, BattleAbilitySet abilities) {
        return new BattleUnitSeed(id, side, slot, hp, attack, attack, 0, 0, speed, 0, 0, DamageChannel.PHYSICAL,
                abilities == null ? BattleAbilitySet.basicOnly(DamageChannel.PHYSICAL) : abilities);
    }

    private static BattleUnitSeed unitWithPassives(String id, TeamSide side, int slot, long hp, long attack, int speed,
                                                   List<BattlePassive> passives) {
        return new BattleUnitSeed(id, side, slot, hp, attack, attack, 0, 0, speed, 0, 0, DamageChannel.PHYSICAL,
                BattleAbilitySet.basicOnly(DamageChannel.PHYSICAL), passives);
    }
}
