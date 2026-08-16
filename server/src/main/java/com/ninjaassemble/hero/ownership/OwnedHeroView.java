package com.ninjaassemble.hero.ownership;

import java.util.UUID;

public record OwnedHeroView(
        UUID id,
        String characterId,
        String displayName,
        int level,
        long exp,
        String frameTier,
        String currentVariant,
        int awakeningLevel
) {}
