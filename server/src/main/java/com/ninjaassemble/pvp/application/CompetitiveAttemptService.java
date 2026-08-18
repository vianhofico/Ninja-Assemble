package com.ninjaassemble.pvp.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UTC-daily competitive attempt quota shared by the canonical Arena/Shadow paths. */
@Service
public final class CompetitiveAttemptService {
    public static final int ARENA_DAILY_ATTEMPTS = 5;
    public static final int SHADOW_DAILY_ATTEMPTS = 3;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CompetitiveAttemptService(JdbcTemplate jdbc, Clock clock) { this.jdbc=jdbc; this.clock=clock; }

    @Transactional(readOnly=true)
    public AttemptState state(UUID playerId, CompetitiveSeasonService.Mode mode, String seasonId) {
        int limit=limit(mode); int used=used(playerId,mode,seasonId,gameDate());
        return new AttemptState(limit,used,Math.max(0,limit-used),gameDate().toString());
    }

    @Transactional
    public AttemptState consume(UUID playerId, CompetitiveSeasonService.Mode mode, String seasonId) {
        String key="competitive-attempt:"+mode.name()+":"+playerId;
        jdbc.queryForList("select pg_advisory_xact_lock(hashtext(?))",key);
        LocalDate date=gameDate(); int limit=limit(mode); int used=used(playerId,mode,seasonId,date);
        if(used>=limit) throw new IllegalStateException(mode.name()+" daily attempts exhausted");
        jdbc.update("""
                insert into competitive_daily_attempts(player_id,mode,season_id,game_date,attempts_used,updated_at)
                values (?,?,?,?,1,now())
                on conflict (player_id,mode,season_id,game_date)
                do update set attempts_used=competitive_daily_attempts.attempts_used+1,updated_at=excluded.updated_at
                """,playerId,mode.name(),seasonId,date);
        return new AttemptState(limit,used+1,Math.max(0,limit-used-1),date.toString());
    }

    private int used(UUID playerId,CompetitiveSeasonService.Mode mode,String seasonId,LocalDate date){
        List<Integer> rows=jdbc.query("select attempts_used from competitive_daily_attempts where player_id=? and mode=? and season_id=? and game_date=?",
                (rs,row)->rs.getInt(1),playerId,mode.name(),seasonId,date);return rows.isEmpty()?0:rows.get(0);
    }
    private static int limit(CompetitiveSeasonService.Mode mode){return mode==CompetitiveSeasonService.Mode.ARENA?ARENA_DAILY_ATTEMPTS:SHADOW_DAILY_ATTEMPTS;}
    private LocalDate gameDate(){return clock.instant().atZone(ZoneOffset.UTC).toLocalDate();}
    public record AttemptState(int dailyAttemptLimit,int attemptsUsed,int attemptsRemaining,String gameDate){}
}
