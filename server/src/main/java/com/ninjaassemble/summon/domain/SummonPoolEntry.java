package com.ninjaassemble.summon.domain;

public record SummonPoolEntry(String heroVariantId, SummonRarity rarity, int weight, boolean featured) {
    public SummonPoolEntry {
        if (heroVariantId == null || heroVariantId.isBlank() || rarity == null || weight <= 0) throw new IllegalArgumentException("invalid summon pool entry");
    }
}
