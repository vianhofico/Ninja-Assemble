package com.ninjaassemble.summon.domain;

import java.util.Map;

public record DuplicateConversionProfile(String version, Map<SummonRarity, Long> soulByRarity) {
    public DuplicateConversionProfile {
        if (version == null || version.isBlank() || soulByRarity == null) throw new IllegalArgumentException("invalid duplicate conversion profile");
        soulByRarity = Map.copyOf(soulByRarity);
    }

    public long soulsFor(SummonRarity rarity) {
        Long value = soulByRarity.get(rarity);
        if (value == null || value < 0) throw new IllegalStateException("duplicate conversion missing for " + rarity);
        return value;
    }
}
