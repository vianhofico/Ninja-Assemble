package com.ninjaassemble.progression.scroll;

import java.util.Map;

public record ScrollDefinition(String id, String nameKey, ScrollElement element, int maxLevel, Map<String, Long> baseStats) {
    public ScrollDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || element == null || maxLevel < 1) throw new IllegalArgumentException("invalid scroll definition");
        baseStats = baseStats == null ? Map.of() : Map.copyOf(baseStats);
    }
}
