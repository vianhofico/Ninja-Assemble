package com.ninjaassemble.reference;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceEvidenceGateTest {
    @Test
    void experimentalProfileMayRemainEmpty() {
        var profile = new BalanceProfileDescriptor(
                "experimental-v1", "DAMAGE", ReferenceConfidence.EXPERIMENTAL, 3, 2, 2);
        assertDoesNotThrow(() -> ReferenceEvidenceGate.requireConfidenceIsSupported(profile, List.of()));
        assertFalse(ReferenceEvidenceGate.canBeVerified(profile, List.of()));
    }

    @Test
    void observedProfileRequiresAtLeastOneSample() {
        var profile = new BalanceProfileDescriptor(
                "observed-v1", "DAMAGE", ReferenceConfidence.OBSERVED, 3, 2, 2);
        assertThrows(IllegalStateException.class,
                () -> ReferenceEvidenceGate.requireConfidenceIsSupported(profile, List.of()));
    }

    @Test
    void verifiedProfileRequiresSamplesContextsAndIndependentEvidenceRefs() {
        var profile = new BalanceProfileDescriptor(
                "verified-v1", "DAMAGE", ReferenceConfidence.VERIFIED, 3, 2, 2);
        var enough = List.of(
                new ReferenceEvidenceSample("m1", "verified-v1", "ctx-a", "capture-001"),
                new ReferenceEvidenceSample("m2", "verified-v1", "ctx-a", "capture-002"),
                new ReferenceEvidenceSample("m3", "verified-v1", "ctx-b", "capture-002")
        );
        assertTrue(ReferenceEvidenceGate.canBeVerified(profile, enough));
        assertDoesNotThrow(() -> ReferenceEvidenceGate.requireConfidenceIsSupported(profile, enough));

        var notEnough = List.of(
                new ReferenceEvidenceSample("m1", "verified-v1", "ctx-a", "capture-001"),
                new ReferenceEvidenceSample("m2", "verified-v1", "ctx-a", "capture-001"),
                new ReferenceEvidenceSample("m3", "verified-v1", "ctx-a", "capture-001")
        );
        assertFalse(ReferenceEvidenceGate.canBeVerified(profile, notEnough));
        assertThrows(IllegalStateException.class,
                () -> ReferenceEvidenceGate.requireConfidenceIsSupported(profile, notEnough));
    }

    @Test
    void duplicateMeasurementIdsAreRejected() {
        var profile = new BalanceProfileDescriptor(
                "profile", "DAMAGE", ReferenceConfidence.EXPERIMENTAL, 0, 0, 0);
        var duplicate = List.of(
                new ReferenceEvidenceSample("m1", "profile", "a", "r1"),
                new ReferenceEvidenceSample("m1", "profile", "b", "r2")
        );
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceEvidenceGate.summarize(profile, duplicate));
    }
}
