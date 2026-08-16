package com.ninjaassemble.progression.scroll;

public final class ScrollCombineService {
    public CombineResult combine(ScrollInventoryEntry entry, ScrollCombineProfile profile) {
        if (entry == null || profile == null) throw new IllegalArgumentException("entry/profile required");
        if (entry.level() >= profile.maxLevel()) throw new IllegalStateException("scroll already at max level");
        if (entry.quantity() < profile.copiesRequired()) throw new IllegalStateException("not enough copies");
        return new CombineResult(
                new ScrollInventoryEntry(entry.definitionId(), entry.element(), entry.level() + 1, 1),
                entry.quantity() - profile.copiesRequired(),
                profile.version()
        );
    }

    public record CombineResult(ScrollInventoryEntry upgraded, int remainingCopies, String profileVersion) {}
}
