package com.ninjaassemble.progression.scroll;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ScrollLoadout {
    private final EnumMap<ScrollElement, EquippedScroll> slots = new EnumMap<>(ScrollElement.class);

    public void equip(ScrollDefinition definition, int level) {
        if (definition == null) throw new IllegalArgumentException("scroll definition required");
        if (level < 1 || level > definition.maxLevel()) throw new IllegalArgumentException("scroll level outside definition range");
        slots.put(definition.element(), new EquippedScroll(definition.id(), level));
    }

    public Optional<EquippedScroll> at(ScrollElement element) { return Optional.ofNullable(slots.get(element)); }
    public Map<ScrollElement, EquippedScroll> snapshot() { return Map.copyOf(slots); }

    public record EquippedScroll(String definitionId, int level) {}
}
