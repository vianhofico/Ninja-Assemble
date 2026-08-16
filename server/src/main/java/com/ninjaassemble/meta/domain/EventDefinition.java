package com.ninjaassemble.meta.domain;

import java.time.Instant;
import java.util.List;

public record EventDefinition(String id, String nameKey, Instant startsAt, Instant endsAt, List<QuestDefinition> objectives) {
    public EventDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || startsAt == null || endsAt == null || !endsAt.isAfter(startsAt) || objectives == null) throw new IllegalArgumentException("invalid event");
        objectives = List.copyOf(objectives);
    }

    public boolean activeAt(Instant now) { return now != null && !now.isBefore(startsAt) && now.isBefore(endsAt); }
}
