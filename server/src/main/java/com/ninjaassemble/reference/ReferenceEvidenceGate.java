package com.ninjaassemble.reference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class ReferenceEvidenceGate {
    private ReferenceEvidenceGate() {}

    public static EvidenceSummary summarize(BalanceProfileDescriptor profile, Collection<ReferenceEvidenceSample> allSamples) {
        if (profile == null) throw new IllegalArgumentException("profile required");
        if (allSamples == null) throw new IllegalArgumentException("samples required");
        int sampleCount = 0;
        Set<String> contexts = new HashSet<>();
        Set<String> evidenceRefs = new HashSet<>();
        Set<String> measurementIds = new HashSet<>();
        for (ReferenceEvidenceSample sample : allSamples) {
            if (!profile.profileId().equals(sample.profileId())) continue;
            if (!measurementIds.add(sample.measurementId())) {
                throw new IllegalArgumentException("duplicate measurementId: " + sample.measurementId());
            }
            sampleCount++;
            contexts.add(sample.contextKey());
            evidenceRefs.add(sample.evidenceRef());
        }
        return new EvidenceSummary(sampleCount, contexts.size(), evidenceRefs.size());
    }

    public static boolean canBeVerified(BalanceProfileDescriptor profile, Collection<ReferenceEvidenceSample> allSamples) {
        EvidenceSummary summary = summarize(profile, allSamples);
        return summary.samples() >= profile.minSamples()
                && summary.distinctContexts() >= profile.minDistinctContexts()
                && summary.evidenceRefs() >= profile.minEvidenceRefs();
    }

    public static void requireConfidenceIsSupported(BalanceProfileDescriptor profile, Collection<ReferenceEvidenceSample> allSamples) {
        EvidenceSummary summary = summarize(profile, allSamples);
        if (profile.confidence() == ReferenceConfidence.OBSERVED && summary.samples() == 0) {
            throw new IllegalStateException(profile.profileId() + " is OBSERVED but has no evidence samples");
        }
        if (profile.confidence() == ReferenceConfidence.VERIFIED && !canBeVerified(profile, allSamples)) {
            throw new IllegalStateException(
                    profile.profileId() + " is VERIFIED without sufficient evidence: samples=" + summary.samples()
                            + ", contexts=" + summary.distinctContexts() + ", evidenceRefs=" + summary.evidenceRefs());
        }
    }

    public record EvidenceSummary(int samples, int distinctContexts, int evidenceRefs) {}
}
