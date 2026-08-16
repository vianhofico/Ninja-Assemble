package com.ninjaassemble.progression.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EvolutionGateTest {
    @Test
    void gateReportsMissingRequirementsWithoutHardCodingCharacterLogic() {
        EvolutionRequirement requirement = new EvolutionRequirement(80, FrameTier.SIX_PATH, 1, Set.of("quest:ninja-s-rank"), Map.of("class-s-shard", 50L));
        HeroEvolutionContext blocked = new HeroEvolutionContext(80, FrameTier.SIX_PATH, 1, Set.of(), Map.of("class-s-shard", 20L));
        assertFalse(EvolutionGate.evaluate(requirement, blocked).allowed());
        HeroEvolutionContext ready = new HeroEvolutionContext(80, FrameTier.SIX_PATH, 1, Set.of("quest:ninja-s-rank"), Map.of("class-s-shard", 50L));
        assertTrue(EvolutionGate.evaluate(requirement, ready).allowed());
    }
}
