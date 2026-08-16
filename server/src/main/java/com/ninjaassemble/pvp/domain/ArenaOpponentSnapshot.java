package com.ninjaassemble.pvp.domain;

import java.time.Instant;
import java.util.UUID;

public record ArenaOpponentSnapshot(
        String snapshotId,
        UUID playerId,
        String displayName,
        long rating,
        ArenaFormationSnapshot defense,
        Instant capturedAt
) {
    public ArenaOpponentSnapshot {
        if (snapshotId == null || snapshotId.isBlank() || playerId == null || displayName == null || displayName.isBlank() || rating < 0 || defense == null || capturedAt == null) {
            throw new IllegalArgumentException("invalid opponent snapshot");
        }
    }
}
