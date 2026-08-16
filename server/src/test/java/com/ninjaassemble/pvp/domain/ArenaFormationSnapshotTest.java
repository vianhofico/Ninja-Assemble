package com.ninjaassemble.pvp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArenaFormationSnapshotTest {
    @Test
    void arenaSnapshotRequiresExactlyFiveUniqueHeroes() {
        ArenaFormationSnapshot valid = new ArenaFormationSnapshot(List.of(member("1"), member("2"), member("3"), member("4"), member("5")));
        assertEquals(5, valid.members().size());
        assertThrows(IllegalArgumentException.class, () -> new ArenaFormationSnapshot(List.of(member("1"), member("2"))));
        assertThrows(IllegalArgumentException.class, () -> new ArenaFormationSnapshot(List.of(member("1"), member("1"), member("3"), member("4"), member("5"))));
    }

    private static FormationMemberSnapshot member(String id) { return new FormationMemberSnapshot(id, "hero-" + id, null, 10); }
}
