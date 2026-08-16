package com.ninjaassemble.pvp.domain;

public record ArenaRatingProfile(String version, int winDelta, int lossDelta, int minimumRating) {
    public ArenaRatingProfile {
        if (version == null || version.isBlank() || winDelta < 0 || lossDelta > 0 || minimumRating < 0) throw new IllegalArgumentException("invalid rating profile");
    }

    public static ArenaRatingProfile experimentalV1() {
        return new ArenaRatingProfile("experimental-v1-unverified", 10, -5, 0);
    }
}
