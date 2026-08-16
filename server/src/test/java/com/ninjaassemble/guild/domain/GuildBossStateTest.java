package com.ninjaassemble.guild.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GuildBossStateTest {
    @Test
    void damageIsCappedByRemainingBossHp() {
        GuildBossState state = new GuildBossState("boss", 100, 30, 70);
        var result = state.apply(UUID.randomUUID(), 50);
        assertEquals(30, result.appliedDamage());
        assertEquals(0, result.state().currentHp());
        assertEquals(100, result.state().totalDamage());
    }
}
