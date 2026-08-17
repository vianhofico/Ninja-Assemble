package com.ninjaassemble.battle.sim;

import java.util.List;

/** Canonical request contract for deterministic continuous-time combat. */
public record RealtimeBattleRequest(long seed, BattleRuleset ruleset, List<BattleUnitSeed> units) {
    public RealtimeBattleRequest {
        if (ruleset == null || units == null || units.isEmpty()) {
            throw new IllegalArgumentException("ruleset and units required");
        }
        units = List.copyOf(units);
        long teamA = units.stream().filter(it -> it.side() == TeamSide.A).count();
        long teamB = units.stream().filter(it -> it.side() == TeamSide.B).count();
        if (teamA < 1 || teamA > 5 || teamB < 1 || teamB > 5) {
            throw new IllegalArgumentException("each team requires one to five units");
        }
    }
}
