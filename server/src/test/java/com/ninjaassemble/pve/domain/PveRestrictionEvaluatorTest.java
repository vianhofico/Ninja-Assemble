package com.ninjaassemble.pve.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PveRestrictionEvaluatorTest {
    @Test
    void kunoichiRestrictionRejectsNonFemaleTaggedHero() {
        PveModeDefinition mode = new PveModeDefinition("kunoichi", PveModeType.NINJA_TRIAL_PATH_OF_KUNOICHI, 5, 0, null, Set.of(PveRestriction.FEMALE_ONLY), "reward");
        assertTrue(PveRestrictionEvaluator.evaluateTeam(mode, List.of(new PveTeamMember("sakura", Set.of("gender:female")))).allowed());
        assertFalse(PveRestrictionEvaluator.evaluateTeam(mode, List.of(new PveTeamMember("naruto", Set.of("gender:male")))).allowed());
    }
}
