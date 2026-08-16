package com.ninjaassemble.pvp.domain;

public record FormationMemberSnapshot(String playerHeroId, String heroId, boolean awakened, long power) {
    public FormationMemberSnapshot {
        if (playerHeroId == null || playerHeroId.isBlank() || heroId == null || heroId.isBlank() || power < 0)
            throw new IllegalArgumentException("invalid formation member snapshot");
    }
}
