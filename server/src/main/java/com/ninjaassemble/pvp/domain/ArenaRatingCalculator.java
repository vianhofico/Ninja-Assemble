package com.ninjaassemble.pvp.domain;

public final class ArenaRatingCalculator {
    private ArenaRatingCalculator() {}

    public static RatingResult resolve(long currentRating, boolean won, ArenaRatingProfile profile) {
        if (currentRating < 0 || profile == null) throw new IllegalArgumentException("invalid rating input");
        long delta = won ? profile.winDelta() : profile.lossDelta();
        long after = Math.max(profile.minimumRating(), currentRating + delta);
        return new RatingResult(currentRating, after, after - currentRating, profile.version());
    }

    public record RatingResult(long before, long after, long delta, String profileVersion) {}
}
