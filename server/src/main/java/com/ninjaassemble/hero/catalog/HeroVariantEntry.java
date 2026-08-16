package com.ninjaassemble.hero.catalog;

public record HeroVariantEntry(String characterId, String variant, String status) {
    public HeroVariantEntry {
        if (characterId == null || characterId.isBlank() || variant == null || variant.isBlank() || status == null || status.isBlank()) {
            throw new IllegalArgumentException("invalid variant entry");
        }
    }
}
