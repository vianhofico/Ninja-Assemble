package com.ninjaassemble.campaign.domain;

import java.util.List;
import java.util.Set;

public record StageDefinition(
        String id,
        int chapter,
        int index,
        CampaignDifficulty difficulty,
        int energyCost,
        int minPlayerLevel,
        Set<String> prerequisiteStageIds,
        List<WaveDefinition> waves,
        RewardBundle firstClearReward,
        RewardBundle repeatReward
) {
    public StageDefinition {
        if (id == null || id.isBlank() || chapter < 1 || index < 1 || difficulty == null || energyCost < 0 || minPlayerLevel < 1) throw new IllegalArgumentException("invalid stage");
        prerequisiteStageIds = prerequisiteStageIds == null ? Set.of() : Set.copyOf(prerequisiteStageIds);
        waves = waves == null ? List.of() : List.copyOf(waves);
        if (waves.isEmpty()) throw new IllegalArgumentException("stage must have at least one wave");
        if (firstClearReward == null || repeatReward == null) throw new IllegalArgumentException("stage rewards required");
    }
}
