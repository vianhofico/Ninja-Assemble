package com.ninjaassemble.campaign.domain;

public record EnemySlotDefinition(int slot, String enemyDefinitionId, int level, String variantId) {
    public EnemySlotDefinition {
        if (slot < 0 || slot > 4 || enemyDefinitionId == null || enemyDefinitionId.isBlank() || level < 1) throw new IllegalArgumentException("invalid enemy slot");
    }
}
