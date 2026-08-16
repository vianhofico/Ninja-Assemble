package com.ninjaassemble.hero.kit;

import com.ninjaassemble.battle.domain.DamageChannel;
import java.util.Set;

public record TechniqueDefinition(
        String id,
        String nameEn,
        String nameVi,
        String descriptionEn,
        String descriptionVi,
        DamageChannel channel,
        TechniqueKind kind,
        Set<String> tags
) {
    public TechniqueDefinition {
        if (id == null || id.isBlank() || nameEn == null || nameEn.isBlank() || nameVi == null || nameVi.isBlank()
                || descriptionEn == null || descriptionEn.isBlank() || descriptionVi == null || descriptionVi.isBlank()
                || channel == null || kind == null) throw new IllegalArgumentException("invalid technique definition");
        tags = tags == null ? Set.of() : Set.copyOf(tags);
    }
}
