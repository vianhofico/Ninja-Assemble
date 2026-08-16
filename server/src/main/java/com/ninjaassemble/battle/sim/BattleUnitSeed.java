package com.ninjaassemble.battle.sim;

import com.ninjaassemble.battle.domain.DamageChannel;

public record BattleUnitSeed(
        String id,
        TeamSide side,
        int slot,
        long maxHp,
        long physicalAttack,
        long chakraAttack,
        long physicalDefense,
        long chakraDefense,
        int speed,
        int physicalCritBps,
        int chakraCritBps,
        DamageChannel primaryChannel
) {
    public BattleUnitSeed {
        if (id == null || id.isBlank() || side == null || primaryChannel == null) throw new IllegalArgumentException("unit identity required");
        if (slot < 0 || slot > 4 || maxHp <= 0 || speed <= 0) throw new IllegalArgumentException("invalid slot/hp/speed");
        if (physicalAttack < 0 || chakraAttack < 0 || physicalDefense < 0 || chakraDefense < 0) throw new IllegalArgumentException("negative stat");
        if (physicalCritBps < 0 || physicalCritBps > 10_000 || chakraCritBps < 0 || chakraCritBps > 10_000) throw new IllegalArgumentException("invalid crit chance");
    }
}
