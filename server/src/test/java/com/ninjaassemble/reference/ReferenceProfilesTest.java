package com.ninjaassemble.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceProfilesTest {
    @Test
    void allCurrentRuntimeProfilesRemainExplicitlyExperimental() {
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.COMBAT_STATS));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.DAMAGE_FORMULA));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.COMPLETE_ROSTER_SUMMON));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.HERO_LEVEL_COST));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.ABILITY_CYCLE));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.STRUCTURED_EFFECTS));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.TECHNIQUE_MAPPING));
        assertEquals(ReferenceConfidence.EXPERIMENTAL, ReferenceProfiles.confidence(ReferenceProfiles.PASSIVE_LIFECYCLE));
    }

    @Test
    void unknownProfilesCannotBeSilentlyTreatedAsVerified() {
        assertThrows(IllegalArgumentException.class, () -> ReferenceProfiles.confidence("unknown-profile"));
    }
}
