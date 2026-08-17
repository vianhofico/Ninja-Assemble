package com.ninjaassemble.battle.sim;

import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import java.util.List;

/** Passive definition using event/time-based triggers only. */
public record BattlePassive(
        String id,
        PassiveTrigger trigger,
        List<SkillEffectDefinition> effects,
        boolean oncePerBattle,
        int thresholdBps,
        long intervalMs
) {
    public BattlePassive {
        if (id == null || id.isBlank() || trigger == null) throw new IllegalArgumentException("passive identity required");
        effects = effects == null ? List.of() : List.copyOf(effects);
        if (effects.isEmpty()) throw new IllegalArgumentException("passive needs at least one effect");
        if (thresholdBps < 0 || thresholdBps > 10_000) throw new IllegalArgumentException("invalid passive threshold");
        if (intervalMs < 0) throw new IllegalArgumentException("intervalMs cannot be negative");
        if (trigger == PassiveTrigger.TIME_INTERVAL && intervalMs <= 0) throw new IllegalArgumentException("TIME_INTERVAL passive requires intervalMs > 0");
    }

    public BattlePassive(String id, PassiveTrigger trigger, List<SkillEffectDefinition> effects,
                         boolean oncePerBattle, int thresholdBps) {
        this(id, trigger, effects, oncePerBattle, thresholdBps, 0L);
    }
}
