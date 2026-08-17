package com.ninjaassemble.play.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PlayableBattleServiceRealtimeTest {
    @Test
    void starsUseBattleDurationInsteadOfLegacyRoundCount() {
        assertEquals(3, PlayableBattleService.starsForDuration(20_000L, 1));
        assertEquals(2, PlayableBattleService.starsForDuration(20_001L, 1));
        assertEquals(2, PlayableBattleService.starsForDuration(40_000L, 1));
        assertEquals(1, PlayableBattleService.starsForDuration(40_001L, 1));
    }

    @Test
    void durationThresholdScalesWithWaveCount() {
        assertEquals(3, PlayableBattleService.starsForDuration(60_000L, 3));
        assertEquals(2, PlayableBattleService.starsForDuration(60_001L, 3));
        assertEquals(2, PlayableBattleService.starsForDuration(120_000L, 3));
        assertEquals(1, PlayableBattleService.starsForDuration(120_001L, 3));
    }

    @Test
    void invalidDurationInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PlayableBattleService.starsForDuration(-1L, 1));
        assertThrows(IllegalArgumentException.class, () -> PlayableBattleService.starsForDuration(1L, 0));
    }
}
