package com.ninjaassemble.meta.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ObjectiveProgressTest {
    @Test
    void progressCapsAtTargetAndClaimsOnce() {
        ObjectiveProgress progress = new ObjectiveProgress(new ObjectiveDefinition(ObjectiveType.CLEAR_STAGE, null, 3), 0, false);
        progress = progress.add(5);
        assertEquals(3, progress.current());
        ObjectiveProgress claimed = progress.claim();
        assertThrows(IllegalStateException.class, claimed::claim);
    }
}
