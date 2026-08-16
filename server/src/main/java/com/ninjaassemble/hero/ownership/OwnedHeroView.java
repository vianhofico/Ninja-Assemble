package com.ninjaassemble.hero.ownership;

import java.util.UUID;

public record OwnedHeroView(
        UUID id,
        String characterId,
        String heroId,
        String displayName,
        int level,
        long exp,
        String frameTier,
        boolean awakened,
        String awakeningId,
        String awakeningName,
        // Compatibility views for pre-M44 callers. These are no longer the source of truth.
        String currentVariant,
        int awakeningLevel
) {}
