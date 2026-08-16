package com.ninjaassemble.hero.domain;

public record HeroStats(
        long maxHp,
        long physicalAttack,
        long chakraAttack,
        long physicalDefense,
        long chakraDefense,
        int speed,
        int physicalCritBps,
        int chakraCritBps
) {
    public HeroStats {
        if (maxHp <= 0 || speed <= 0) throw new IllegalArgumentException("hp and speed must be positive");
        if (physicalAttack < 0 || chakraAttack < 0 || physicalDefense < 0 || chakraDefense < 0) throw new IllegalArgumentException("stats cannot be negative");
        if (physicalCritBps < 0 || physicalCritBps > 10_000 || chakraCritBps < 0 || chakraCritBps > 10_000) throw new IllegalArgumentException("crit chance outside basis-point range");
    }
}
