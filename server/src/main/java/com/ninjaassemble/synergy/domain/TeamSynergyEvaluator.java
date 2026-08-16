package com.ninjaassemble.synergy.domain;

import java.util.List;
import java.util.Set;

public final class TeamSynergyEvaluator {
    private TeamSynergyEvaluator() {}

    public static List<TeamSynergyDefinition> active(List<TeamSynergyDefinition> definitions, Set<String> teamCharacterIds) {
        if (definitions == null || teamCharacterIds == null) throw new IllegalArgumentException("definitions/team required");
        return definitions.stream().filter(definition -> {
            long matches = definition.requiredCharacterIds().stream().filter(teamCharacterIds::contains).count();
            return matches >= definition.minimumMembers();
        }).toList();
    }
}
