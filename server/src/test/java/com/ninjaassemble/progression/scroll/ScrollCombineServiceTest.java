package com.ninjaassemble.progression.scroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ScrollCombineServiceTest {
    @Test
    void profileControlsCombineRules() {
        ScrollCombineService service = new ScrollCombineService();
        ScrollInventoryEntry entry = new ScrollInventoryEntry("fire-scroll", ScrollElement.FIRE, 1, 4);
        var result = service.combine(entry, new ScrollCombineProfile("test", 3, 10));
        assertEquals(2, result.upgraded().level());
        assertEquals(1, result.remainingCopies());
        assertThrows(IllegalStateException.class, () -> service.combine(new ScrollInventoryEntry("fire-scroll", ScrollElement.FIRE, 1, 2), new ScrollCombineProfile("test", 3, 10)));
    }
}
