package com.ninjaassemble.battle.sim;

import java.util.List;
import java.util.Map;

public record BattleResult(
        long seed,
        String rulesetVersion,
        BattleOutcome outcome,
        long durationMs,
        List<BattleEvent> events,
        Map<String, Long> finalHp
) {}
