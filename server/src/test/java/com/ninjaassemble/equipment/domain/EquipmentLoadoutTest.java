package com.ninjaassemble.equipment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EquipmentLoadoutTest {
    @Test
    void sameSlotIsReplacedDeterministically() {
        EquipmentDefinition one = new EquipmentDefinition("kunai-a", "item.kunai", EquipmentSlot.WEAPON, "R", 10, null, Map.of());
        EquipmentDefinition two = new EquipmentDefinition("kunai-b", "item.kunai2", EquipmentSlot.WEAPON, "SR", 20, null, Map.of());
        EquipmentLoadout loadout = new EquipmentLoadout();
        loadout.equip(one, new EquipmentInstance(UUID.randomUUID(), "kunai-a", 0, 0, Map.of()));
        loadout.equip(two, new EquipmentInstance(UUID.randomUUID(), "kunai-b", 0, 0, Map.of()));
        assertEquals("kunai-b", loadout.snapshot().get(EquipmentSlot.WEAPON).definitionId());
    }
}
