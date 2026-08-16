package com.ninjaassemble.progression.tailedbeast;

import java.util.Set;

public record JinchurikiDefinition(
        String characterId,
        TailedBeast beast,
        Set<String> compatibleVariantIds,
        String progressionTrackId
) {
    public JinchurikiDefinition {
        if (characterId == null || characterId.isBlank() || beast == null || progressionTrackId == null || progressionTrackId.isBlank()) throw new IllegalArgumentException("invalid jinchuriki definition");
        compatibleVariantIds = compatibleVariantIds == null ? Set.of() : Set.copyOf(compatibleVariantIds);
    }
}
