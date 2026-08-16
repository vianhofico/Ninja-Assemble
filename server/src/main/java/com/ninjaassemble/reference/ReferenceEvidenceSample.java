package com.ninjaassemble.reference;

public record ReferenceEvidenceSample(
        String measurementId,
        String profileId,
        String contextKey,
        String evidenceRef
) {
    public ReferenceEvidenceSample {
        if (measurementId == null || measurementId.isBlank()) throw new IllegalArgumentException("measurementId required");
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profileId required");
        if (contextKey == null || contextKey.isBlank()) throw new IllegalArgumentException("contextKey required");
        if (evidenceRef == null || evidenceRef.isBlank()) throw new IllegalArgumentException("evidenceRef required");
    }
}
