package com.ninjaassemble.pvp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArenaRatingCalculatorTest {
    @Test
    void experimentalArenaProfileUsesDeterministicWinLossDeltasAndFloor() {
        ArenaRatingProfile profile = ArenaRatingProfile.experimentalV1();
        var win = ArenaRatingCalculator.resolve(1_000, true, profile);
        var loss = ArenaRatingCalculator.resolve(1_000, false, profile);
        var floor = ArenaRatingCalculator.resolve(0, false, profile);
        assertEquals(10, win.delta());
        assertEquals(1_010, win.after());
        assertEquals(-5, loss.delta());
        assertEquals(995, loss.after());
        assertEquals(0, floor.after());
        assertEquals("experimental-v1-unverified", profile.version());
    }
}
