package com.ninjaassemble.progression.scroll;

public record ScrollInventoryEntry(String definitionId, ScrollElement element, int level, int quantity) {
    public ScrollInventoryEntry {
        if (definitionId == null || definitionId.isBlank() || element == null || level < 1 || quantity < 1) throw new IllegalArgumentException("invalid scroll inventory entry");
    }
}
