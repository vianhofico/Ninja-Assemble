package com.ninjaassemble.synergy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TeamSynergyEvaluatorTest {
    @Test
    void teamSevenRequiresConfiguredMemberCount() {
        TeamSynergyDefinition definition = new TeamSynergyDefinition("team7", "synergy.team7",
                Set.of("naruto", "sasuke", "sakura", "kakashi"), 4,
                List.of(new SynergyStatBonus("attack", 500)));
        assertEquals(0, TeamSynergyEvaluator.active(List.of(definition), Set.of("naruto", "sasuke", "sakura")).size());
        assertEquals(1, TeamSynergyEvaluator.active(List.of(definition), Set.of("naruto", "sasuke", "sakura", "kakashi")).size());
    }
}
