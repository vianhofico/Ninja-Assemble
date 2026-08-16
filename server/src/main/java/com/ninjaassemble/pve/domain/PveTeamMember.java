package com.ninjaassemble.pve.domain;

import java.util.Set;

public record PveTeamMember(String playerHeroId, Set<String> tags) {
    public PveTeamMember {
        if (playerHeroId == null || playerHeroId.isBlank()) throw new IllegalArgumentException("hero id required");
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }

    public boolean hasTag(String tag) { return tags.contains(tag); }
}
