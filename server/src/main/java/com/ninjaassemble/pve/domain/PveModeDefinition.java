package com.ninjaassemble.pve.domain;

import java.util.Set;

public record PveModeDefinition(
        String id,
        PveModeType type,
        int teamSize,
        int energyCost,
        Integer dailyAttemptLimit,
        Set<PveRestriction> restrictions,
        String rewardTableId
) {
    public PveModeDefinition {
        if (id == null || id.isBlank() || type == null || teamSize < 1 || teamSize > 5 || energyCost < 0 || rewardTableId == null || rewardTableId.isBlank()) throw new IllegalArgumentException("invalid pve mode");
        if (dailyAttemptLimit != null && dailyAttemptLimit < 1) throw new IllegalArgumentException("invalid attempt limit");
        restrictions = restrictions == null ? Set.of() : Set.copyOf(restrictions);
    }
}
