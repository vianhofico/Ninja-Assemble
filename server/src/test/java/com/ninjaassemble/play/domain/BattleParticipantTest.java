package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.sim.TeamSide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleParticipantTest {
    @Test
    void keepsPresentationIdentityAndBattleState() {
        var participant = new BattleParticipant(
                "unit-1", "naruto-uzumaki", "Naruto Uzumaki", "Sage Mode", 80, TeamSide.A, 2, 12500);
        assertEquals("naruto-uzumaki", participant.characterId());
        assertEquals("Sage Mode", participant.variant());
        assertEquals(2, participant.slot());
        assertEquals(12500, participant.maxHp());
    }

    @Test
    void rejectsInvalidBattleSlotAndHp() {
        assertThrows(IllegalArgumentException.class, () -> new BattleParticipant(
                "unit", "naruto-uzumaki", "Naruto", null, 1, TeamSide.A, 5, 100));
        assertThrows(IllegalArgumentException.class, () -> new BattleParticipant(
                "unit", "naruto-uzumaki", "Naruto", null, 1, TeamSide.A, 0, 0));
    }
}
