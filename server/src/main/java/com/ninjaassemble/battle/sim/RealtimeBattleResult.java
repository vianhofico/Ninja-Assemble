package com.ninjaassemble.battle.sim;

import java.util.List;
import java.util.Map;

public record RealtimeBattleResult(
        long seed,
        String rulesetVersion,
        BattleOutcome outcome,
        long durationMs,
        List<RealtimeBattleEvent> events,
        Map<String, Long> finalHp
) {
    public RealtimeBattleResult {
        events = events == null ? List.of() : List.copyOf(events);
        finalHp = finalHp == null ? Map.of() : Map.copyOf(finalHp);
    }
}
