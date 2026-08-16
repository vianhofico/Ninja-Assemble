package com.ninjaassemble.progression.domain;

public record EvolutionPathDefinition(
        String id,
        String fromVariantId,
        String toVariantId,
        EvolutionRequirement requirement
) {
    public EvolutionPathDefinition {
        if (id == null || id.isBlank() || fromVariantId == null || fromVariantId.isBlank() || toVariantId == null || toVariantId.isBlank() || requirement == null) {
            throw new IllegalArgumentException("invalid evolution path");
        }
        if (fromVariantId.equals(toVariantId)) throw new IllegalArgumentException("evolution variants must differ");
    }
}
