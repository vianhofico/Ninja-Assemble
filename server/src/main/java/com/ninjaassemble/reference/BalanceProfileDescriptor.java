package com.ninjaassemble.reference;

public record BalanceProfileDescriptor(
        String profileId,
        String category,
        ReferenceConfidence confidence,
        int minSamples,
        int minDistinctContexts,
        int minEvidenceRefs
) {
    public BalanceProfileDescriptor {
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profileId required");
        if (category == null || category.isBlank()) throw new IllegalArgumentException("category required");
        if (confidence == null) throw new IllegalArgumentException("confidence required");
        if (minSamples < 0 || minDistinctContexts < 0 || minEvidenceRefs < 0) {
            throw new IllegalArgumentException("evidence thresholds must be non-negative");
        }
    }
}
