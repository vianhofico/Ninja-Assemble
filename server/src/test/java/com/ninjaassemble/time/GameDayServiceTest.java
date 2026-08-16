package com.ninjaassemble.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class GameDayServiceTest {
    @Test
    void dayRollsAtConfiguredServerHour() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-16T21:30:00Z"), ZoneOffset.UTC); // 04:30 Bangkok
        GameDayService service = new GameDayService(clock, "Asia/Bangkok", 5);
        assertEquals(LocalDate.of(2026, 8, 16), service.currentGameDate());
    }
}
