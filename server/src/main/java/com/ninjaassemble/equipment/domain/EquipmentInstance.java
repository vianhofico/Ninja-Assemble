package com.ninjaassemble.equipment.domain;

import java.util.Map;
import java.util.UUID;

public record EquipmentInstance(UUID id, String definitionId, int enhanceLevel, int refineLevel, Map<String, Long> rolledStats) {
    public EquipmentInstance {
        if (id == null || definitionId == null || definitionId.isBlank() || enhanceLevel < 0 || refineLevel < 0) throw new IllegalArgumentException("invalid equipment instance");
        rolledStats = rolledStats == null ? Map.of() : Map.copyOf(rolledStats);
    }
}
