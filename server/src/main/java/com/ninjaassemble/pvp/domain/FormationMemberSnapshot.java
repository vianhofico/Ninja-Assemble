package com.ninjaassemble.pvp.domain;

public record FormationMemberSnapshot(String playerHeroId, String heroDefinitionId, String variantId, long power) {
    public FormationMemberSnapshot {
        if (playerHeroId == null || playerHeroId.isBlank() || heroDefinitionId == null || heroDefinitionId.isBlank() || power < 0) throw new IllegalArgumentException("invalid formation member snapshot");
    }
}
