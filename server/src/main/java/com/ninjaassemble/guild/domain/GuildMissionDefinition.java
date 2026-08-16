package com.ninjaassemble.guild.domain;

import com.ninjaassemble.meta.domain.ObjectiveDefinition;

public record GuildMissionDefinition(String id, String nameKey, ObjectiveDefinition objective, long contributionReward) {
    public GuildMissionDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || objective == null || contributionReward < 0) throw new IllegalArgumentException("invalid guild mission");
    }
}
