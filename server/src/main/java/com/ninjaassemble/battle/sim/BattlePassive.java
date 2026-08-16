package com.ninjaassemble.battle.sim;

import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import java.util.List;

public record BattlePassive(
        String id,
        PassiveTrigger trigger,
        List<SkillEffectDefinition> effects,
        boolean oncePerBattle,
        int thresholdBps
) {
    public BattlePassive {
        if (id == null || id.isBlank() || trigger == null) throw new IllegalArgumentException("passive identity required");
        effects = effects == null ? List.of() : List.copyOf(effects);
        if (effects.isEmpty()) throw new IllegalArgumentException("passive needs at least one effect");
        if (thresholdBps < 0 || thresholdBps > 10_000) throw new IllegalArgumentException("invalid passive threshold");
    }
}
