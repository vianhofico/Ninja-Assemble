package com.ninjaassemble.synergy.domain;

public record SynergyStatBonus(String stat, int basisPoints) {
    public SynergyStatBonus {
        if (stat == null || stat.isBlank() || basisPoints == 0) throw new IllegalArgumentException("invalid synergy bonus");
    }
}
