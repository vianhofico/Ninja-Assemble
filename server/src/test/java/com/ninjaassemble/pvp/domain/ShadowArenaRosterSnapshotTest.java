package com.ninjaassemble.pvp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShadowArenaRosterSnapshotTest {
    @Test
    void shadowArenaUsesFifteenUniqueHeroesAndBestOfThree() {
        List<ArenaFormationSnapshot> squads = new ArrayList<>();
        for (int squad = 0; squad < 3; squad++) {
            List<FormationMemberSnapshot> members = new ArrayList<>();
            for (int slot = 0; slot < 5; slot++) {
                String id = "h" + (squad * 5 + slot);
                members.add(new FormationMemberSnapshot(id, id, false, 10));
            }
            squads.add(new ArenaFormationSnapshot(members));
        }
        ShadowArenaRosterSnapshot snapshot = new ShadowArenaRosterSnapshot(squads);
        assertEquals(150, snapshot.totalPower());
        assertEquals(com.ninjaassemble.battle.domain.ShadowArenaSeries.SeriesWinner.PLAYER,
                new ShadowArenaMatchResolver().resolve(List.of(true, false, true)));
    }
}
