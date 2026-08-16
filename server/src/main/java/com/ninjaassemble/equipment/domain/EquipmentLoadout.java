package com.ninjaassemble.equipment.domain;

import java.util.EnumMap;
import java.util.Map;

public final class EquipmentLoadout {
    private final EnumMap<EquipmentSlot, EquipmentInstance> slots = new EnumMap<>(EquipmentSlot.class);

    public void equip(EquipmentDefinition definition, EquipmentInstance instance) {
        if (definition == null || instance == null || !definition.id().equals(instance.definitionId())) throw new IllegalArgumentException("equipment definition/instance mismatch");
        slots.put(definition.slot(), instance);
    }

    public EquipmentInstance unequip(EquipmentSlot slot) { return slots.remove(slot); }
    public Map<EquipmentSlot, EquipmentInstance> snapshot() { return Map.copyOf(slots); }
}
