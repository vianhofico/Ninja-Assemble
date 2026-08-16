package com.ninjaassemble.progression.domain;

import java.util.Map;
import java.util.Set;

public record HeroEvolutionContext(
        int level,
        FrameTier frameTier,
        int framePlus,
        Set<String> flags,
        Map<String, Long> materials
) {
    public HeroEvolutionContext {
        if (level < 1 || frameTier == null || framePlus < 0) throw new IllegalArgumentException("invalid evolution context");
        flags = flags == null ? Set.of() : Set.copyOf(flags);
        materials = materials == null ? Map.of() : Map.copyOf(materials);
    }
}
