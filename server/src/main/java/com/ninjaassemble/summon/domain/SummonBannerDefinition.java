package com.ninjaassemble.summon.domain;

import java.util.List;

public record SummonBannerDefinition(
        String id,
        String version,
        String currency,
        long singleCost,
        int hardPity,
        SummonRarity pityRarity,
        List<SummonPoolEntry> pool
) {
    public SummonBannerDefinition {
        if (id == null || id.isBlank() || version == null || version.isBlank() || currency == null || currency.isBlank() || singleCost < 0 || hardPity < 1 || pityRarity == null || pool == null || pool.isEmpty()) throw new IllegalArgumentException("invalid summon banner");
        pool = List.copyOf(pool);
        if (pool.stream().noneMatch(entry -> entry.rarity().ordinal() >= pityRarity.ordinal())) throw new IllegalArgumentException("pity rarity absent from pool");
    }
}
