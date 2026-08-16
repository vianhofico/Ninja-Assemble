package com.ninjaassemble.progression.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class FrameAdvanceStateTest {
    @Test
    void verifiedAdvanceCountsAreAppliedExactly() {
        FrameAdvanceState state = new FrameAdvanceState(FrameTier.GENIN, 0).advanceOne();
        assertEquals(FrameTier.CHUNIN, state.tier());
        state = state.advanceOne();
        assertEquals(FrameTier.CHUNIN, state.tier());
        assertEquals(1, state.step());
        state = state.advanceOne();
        assertEquals(FrameTier.JONIN, state.tier());
    }

    @Test
    void unverifiedSixPathToAwakeningGateIsNotInvented() {
        assertThrows(IllegalStateException.class, () -> new FrameAdvanceState(FrameTier.SIX_PATH, 0).advanceOne());
    }
}
