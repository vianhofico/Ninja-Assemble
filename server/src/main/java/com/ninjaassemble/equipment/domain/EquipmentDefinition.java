package com.ninjaassemble.equipment.domain;

import java.util.Map;

public record EquipmentDefinition(
        String id,
        String nameKey,
        EquipmentSlot slot,
        String rarity,
        int maxEnhanceLevel,
        String setId,
        Map<String, Long> baseStats
) {
    public EquipmentDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || slot == null || rarity == null || rarity.isBlank() || maxEnhanceLevel < 0) throw new IllegalArgumentException("invalid equipment definition");
        baseStats = baseStats == null ? Map.of() : Map.copyOf(baseStats);
    }
}
