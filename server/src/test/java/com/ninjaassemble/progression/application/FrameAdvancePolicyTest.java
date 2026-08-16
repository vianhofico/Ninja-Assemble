package com.ninjaassemble.progression.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class FrameAdvancePolicyTest {
    @Test
    void followsTheVerifiedEarlyFrameStepCounts() {
        var genin = FrameAdvancePolicy.advance("GENIN", 0);
        assertEquals("CHUNIN", genin.tierAfter());
        assertEquals(0, genin.stepAfter());

        var chuninFirst = FrameAdvancePolicy.advance("CHUNIN", 0);
        assertEquals("CHUNIN", chuninFirst.tierAfter());
        assertEquals(1, chuninFirst.stepAfter());
        var chuninSecond = FrameAdvancePolicy.advance("CHUNIN", 1);
        assertEquals("JONIN", chuninSecond.tierAfter());

        assertEquals("KAGE", FrameAdvancePolicy.advance("JONIN", 2).tierAfter());
        assertEquals("SIX_PATH", FrameAdvancePolicy.advance("KAGE", 3).tierAfter());
    }

    @Test
    void refusesToInventSixPathToAwakeningRules() {
        assertThrows(IllegalStateException.class, () -> FrameAdvancePolicy.advance("SIX_PATH", 0));
    }
}
