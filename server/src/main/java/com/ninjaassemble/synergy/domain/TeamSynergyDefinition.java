package com.ninjaassemble.synergy.domain;

import java.util.List;
import java.util.Set;

public record TeamSynergyDefinition(
        String id,
        String nameKey,
        Set<String> requiredCharacterIds,
        int minimumMembers,
        List<SynergyStatBonus> bonuses
) {
    public TeamSynergyDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || requiredCharacterIds == null || requiredCharacterIds.isEmpty()) throw new IllegalArgumentException("invalid synergy definition");
        requiredCharacterIds = Set.copyOf(requiredCharacterIds);
        if (minimumMembers < 1 || minimumMembers > requiredCharacterIds.size()) throw new IllegalArgumentException("invalid minimum members");
        bonuses = bonuses == null ? List.of() : List.copyOf(bonuses);
    }
}
