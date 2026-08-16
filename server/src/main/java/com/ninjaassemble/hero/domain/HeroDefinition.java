package com.ninjaassemble.hero.domain;

import com.ninjaassemble.battle.domain.NinjaArchetype;
import java.util.List;

public record HeroDefinition(
        String id,
        int version,
        String nameKey,
        String variantKey,
        NinjaArchetype archetype,
        BattlePosition position,
        HeroStats baseStats,
        List<SkillDefinition> skills,
        HeroPresentation presentation
) {
    public HeroDefinition {
        if (id == null || id.isBlank() || version <= 0 || nameKey == null || nameKey.isBlank()) throw new IllegalArgumentException("invalid hero identity");
        if (archetype == null || position == null || baseStats == null) throw new IllegalArgumentException("hero combat metadata required");
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}
