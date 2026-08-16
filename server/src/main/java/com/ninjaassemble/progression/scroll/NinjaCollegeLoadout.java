package com.ninjaassemble.progression.scroll;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class NinjaCollegeLoadout {
    private static final Set<ScrollElement> REQUIRED = Set.of(
            ScrollElement.YIN_YANG, ScrollElement.FIRE, ScrollElement.WATER,
            ScrollElement.WIND, ScrollElement.EARTH, ScrollElement.LIGHTNING);
    private final EnumMap<ScrollElement, ScrollLoadout.EquippedScroll> slots = new EnumMap<>(ScrollElement.class);

    public void inlay(ScrollElement element, String definitionId, int level) {
        if (element == null || definitionId == null || definitionId.isBlank() || level < 1) throw new IllegalArgumentException("invalid scroll slot");
        slots.put(element, new ScrollLoadout.EquippedScroll(definitionId, level));
    }

    public boolean complete() { return slots.keySet().containsAll(REQUIRED); }
    public Map<ScrollElement, ScrollLoadout.EquippedScroll> snapshot() { return Map.copyOf(slots); }
}
