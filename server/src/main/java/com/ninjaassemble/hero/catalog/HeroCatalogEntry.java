package com.ninjaassemble.hero.catalog;

public record HeroCatalogEntry(String id, String character, String group, String scope) {
    public HeroCatalogEntry {
        if (id == null || id.isBlank() || character == null || character.isBlank()) throw new IllegalArgumentException("hero id/name required");
    }
}
