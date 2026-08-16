package com.ninjaassemble.summon.domain;

public record SummonState(int pullsSincePity) {
    public SummonState {
        if (pullsSincePity < 0) throw new IllegalArgumentException("invalid pity state");
    }
}
