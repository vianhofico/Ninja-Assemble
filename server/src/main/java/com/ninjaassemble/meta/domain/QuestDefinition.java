package com.ninjaassemble.meta.domain;

import com.ninjaassemble.campaign.domain.RewardBundle;

public record QuestDefinition(
        String id,
        String nameKey,
        ObjectiveDefinition objective,
        RewardBundle reward,
        ResetCadence cadence
) {
    public QuestDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || objective == null || reward == null || cadence == null) throw new IllegalArgumentException("invalid quest");
    }
}
