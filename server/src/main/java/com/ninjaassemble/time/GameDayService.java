package com.ninjaassemble.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GameDayService {
    private final Clock clock;
    private final ZoneId zone;
    private final int resetHour;

    public GameDayService(Clock clock, @Value("${game.clock.zone:Asia/Bangkok}") String zone, @Value("${game.clock.reset-hour:5}") int resetHour) {
        this.clock = clock; this.zone = ZoneId.of(zone); this.resetHour = resetHour;
    }

    public Instant now() { return clock.instant(); }
    public LocalDate currentGameDate() {
        ZonedDateTime local = now().atZone(zone).minusHours(resetHour);
        return local.toLocalDate();
    }
    public Instant nextReset() {
        ZonedDateTime local = now().atZone(zone);
        ZonedDateTime candidate = local.toLocalDate().atTime(resetHour, 0).atZone(zone);
        if (!candidate.isAfter(local)) candidate = candidate.plusDays(1);
        return candidate.toInstant();
    }
}
