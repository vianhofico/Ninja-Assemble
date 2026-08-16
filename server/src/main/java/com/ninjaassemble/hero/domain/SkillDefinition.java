package com.ninjaassemble.hero.domain;

import java.util.List;

public record SkillDefinition(
        String id,
        String nameKey,
        String descriptionKey,
        SkillKind kind,
        int priority,
        int energyCost,
        int energyGain,
        int cooldownTurns,
        List<SkillEffectDefinition> effects
) {
    public SkillDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || kind == null) throw new IllegalArgumentException("skill identity required");
        if (energyCost < 0 || energyGain < 0 || cooldownTurns < 0) throw new IllegalArgumentException("invalid skill resource values");
        effects = effects == null ? List.of() : List.copyOf(effects);
    }
}
