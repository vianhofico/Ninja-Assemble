package com.ninjaassemble.progression.application;

import java.util.Map;

public final class FrameAdvancePolicy {
    public static final String VERSION = "frame-playable-experimental-v1";
    private static final Map<String, Integer> REQUIRED = Map.of(
            "GENIN", 1,
            "CHUNIN", 2,
            "JONIN", 3,
            "KAGE", 4);
    private static final Map<String, String> NEXT = Map.of(
            "GENIN", "CHUNIN",
            "CHUNIN", "JONIN",
            "JONIN", "KAGE",
            "KAGE", "SIX_PATH");
    private static final Map<String, Integer> RANK = Map.of(
            "GENIN", 0, "CHUNIN", 1, "JONIN", 2, "KAGE", 3, "SIX_PATH", 4, "AWAKENING", 5);

    private FrameAdvancePolicy() {}

    public static AdvanceResult advance(String tier, int currentStep) {
        Integer required = REQUIRED.get(tier);
        if (required == null) throw new IllegalStateException("Frame Advance beyond " + tier + " requires a verified late-game profile");
        if (currentStep < 0 || currentStep >= required) throw new IllegalArgumentException("invalid frame step");
        long goldCost = 250L * (rank(tier) + 1L) * (currentStep + 1L);
        int nextStep = currentStep + 1;
        String nextTier = tier;
        if (nextStep >= required) {
            nextTier = NEXT.get(tier);
            nextStep = 0;
        }
        return new AdvanceResult(tier, currentStep, nextTier, nextStep, required, goldCost, VERSION);
    }

    public static int rank(String tier) {
        Integer value = RANK.get(tier);
        if (value == null) throw new IllegalArgumentException("unknown frame tier: " + tier);
        return value;
    }

    public record AdvanceResult(String tierBefore, int stepBefore, String tierAfter, int stepAfter,
                                int requiredStepsAtTier, long goldCost, String profileVersion) {}
}
