package com.ninjaassemble.progression.domain;

import java.util.Map;
import java.util.Set;

public record EvolutionRequirement(
        int minLevel,
        FrameTier minFrameTier,
        int minFramePlus,
        Set<String> requiredFlags,
        Map<String, Long> requiredMaterials
) {
    public EvolutionRequirement {
        if (minLevel < 1 || minFramePlus < 0 || minFrameTier == null) throw new IllegalArgumentException("invalid evolution requirement");
        requiredFlags = requiredFlags == null ? Set.of() : Set.copyOf(requiredFlags);
        requiredMaterials = requiredMaterials == null ? Map.of() : Map.copyOf(requiredMaterials);
        if (requiredMaterials.values().stream().anyMatch(v -> v == null || v < 0)) throw new IllegalArgumentException("invalid material amount");
    }
}
