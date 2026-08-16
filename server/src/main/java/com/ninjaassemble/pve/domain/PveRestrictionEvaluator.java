package com.ninjaassemble.pve.domain;

import java.util.ArrayList;
import java.util.List;

public final class PveRestrictionEvaluator {
    private PveRestrictionEvaluator() {}

    public static Result evaluateTeam(PveModeDefinition mode, List<PveTeamMember> team) {
        List<String> violations = new ArrayList<>();
        if (team == null || team.isEmpty() || team.size() > mode.teamSize()) violations.add("team-size");
        if (mode.restrictions().contains(PveRestriction.FEMALE_ONLY) && team != null) {
            for (PveTeamMember member : team) if (!member.hasTag("gender:female")) violations.add("female-only:" + member.playerHeroId());
        }
        return new Result(violations.isEmpty(), List.copyOf(violations), mode.restrictions());
    }

    public record Result(boolean allowed, List<String> violations, java.util.Set<PveRestriction> battleModifiers) {}
}
