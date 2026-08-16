package com.ninjaassemble.summon.domain;

public record SummonPoolEntry(String heroId, SummonRarity rarity, int weight, boolean featured) {
    public SummonPoolEntry {
        if (heroId == null || heroId.isBlank() || heroId.contains("::") || rarity == null || weight <= 0) {
            throw new IllegalArgumentException("invalid Hero Version summon pool entry");
        }
    }
}
