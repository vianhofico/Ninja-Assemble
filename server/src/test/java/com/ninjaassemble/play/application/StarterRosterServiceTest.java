package com.ninjaassemble.play.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StarterRosterServiceTest {
    @Test
    void starterRosterIsExactlyFiveUniqueNinjas() {
        assertEquals(5, StarterRosterService.STARTERS.size());
        assertEquals(5, StarterRosterService.STARTERS.stream().distinct().count());
        assertTrue(StarterRosterService.STARTERS.contains("naruto-uzumaki"));
        assertTrue(StarterRosterService.STARTERS.contains("sasuke-uchiha"));
    }
}
