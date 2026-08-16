package com.ninjaassemble.progression.scroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScrollLoadoutTest {
    @Test
    void eachElementHasOneSlotAndNewScrollReplacesSameElement() {
        ScrollLoadout loadout = new ScrollLoadout();
        loadout.equip(new ScrollDefinition("fire-a", "scroll.fire.a", ScrollElement.FIRE, 10, Map.of()), 3);
        loadout.equip(new ScrollDefinition("fire-b", "scroll.fire.b", ScrollElement.FIRE, 10, Map.of()), 2);
        assertEquals("fire-b", loadout.at(ScrollElement.FIRE).orElseThrow().definitionId());
        assertEquals(1, loadout.snapshot().size());
    }
}
