package com.ninjaassemble.pvp.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical competitive facade: UTC daily quotas + dynamic monthly seasons + production battle paths. */
@Service
public final class CompetitiveApplicationService {
    public static final String RESET_POLICY = "UTC_DAILY";
    private final ProductionArenaService arena;
    private final ProductionShadowArenaService shadow;
    private final CompetitiveSeasonService seasons;
    private final CompetitiveAttemptService attempts;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CompetitiveApplicationService(ProductionArenaService arena, ProductionShadowArenaService shadow,
                                         CompetitiveSeasonService seasons, CompetitiveAttemptService attempts,
                                         JdbcTemplate jdbc, Clock clock) {
        this.arena=arena; this.shadow=shadow; this.seasons=seasons; this.attempts=attempts; this.jdbc=jdbc; this.clock=clock;
    }

    @Transactional
    public CompetitiveBoard board(UUID playerId) {
        ProductionArenaService.ArenaState arenaState=arena.state(playerId);
        ProductionShadowArenaService.ShadowArenaState shadowState=shadow.state(playerId);
        return new CompetitiveBoard(gameDate().toString(),RESET_POLICY,
                attempts.state(playerId,CompetitiveSeasonService.Mode.ARENA,arenaState.seasonId()),
                attempts.state(playerId,CompetitiveSeasonService.Mode.SHADOW_ARENA,shadowState.seasonId()),
                arenaState,shadowState);
    }

    @Transactional
    public ProductionArenaService.ArenaBattleView fightArena(UUID playerId,UUID opponentPlayerId,UUID requestId){
        validate(opponentPlayerId,requestId); lock(playerId,CompetitiveSeasonService.Mode.ARENA);
        String seasonId=seasons.currentSeasonId(CompetitiveSeasonService.Mode.ARENA);
        if(!requestExists(requestId,"ARENA")) attempts.consume(playerId,CompetitiveSeasonService.Mode.ARENA,seasonId);
        return arena.fight(playerId,opponentPlayerId,requestId);
    }

    @Transactional
    public ProductionShadowArenaService.ShadowArenaBattleView fightShadowArena(UUID playerId,UUID opponentPlayerId,UUID requestId){
        validate(opponentPlayerId,requestId); lock(playerId,CompetitiveSeasonService.Mode.SHADOW_ARENA);
        String seasonId=seasons.currentSeasonId(CompetitiveSeasonService.Mode.SHADOW_ARENA);
        if(!requestExists(requestId,"SHADOW_ARENA")) attempts.consume(playerId,CompetitiveSeasonService.Mode.SHADOW_ARENA,seasonId);
        return shadow.fight(playerId,opponentPlayerId,requestId);
    }

    @Transactional(readOnly=true)
    public List<LeaderboardEntry> leaderboard(CompetitiveSeasonService.Mode mode){
        String table=mode==CompetitiveSeasonService.Mode.ARENA?"arena_profiles":"shadow_arena_profiles";
        String seasonId=seasons.currentSeasonId(mode);
        return jdbc.query("""
                select ranked.player_id,p.display_name,ranked.rating,ranked.rank from (
                    select player_id,rating,dense_rank() over(order by rating desc,player_id) as rank
                    from %s where season_id=?
                ) ranked join players p on p.id=ranked.player_id
                order by ranked.rank,ranked.player_id limit 100
                """.formatted(table),(rs,row)->new LeaderboardEntry(rs.getObject("player_id",UUID.class),rs.getString("display_name"),rs.getLong("rating"),rs.getLong("rank")),seasonId);
    }

    private boolean requestExists(UUID requestId,String mode){
        Integer count=jdbc.queryForObject("select count(*) from competitive_battle_requests where request_id=? and mode=?",Integer.class,requestId,mode);
        return count!=null&&count>0;
    }
    private void lock(UUID playerId,CompetitiveSeasonService.Mode mode){jdbc.queryForList("select pg_advisory_xact_lock(hashtext(?))","competitive:"+mode.name()+":"+playerId);}
    private static void validate(UUID opponentPlayerId,UUID requestId){if(opponentPlayerId==null)throw new IllegalArgumentException("opponentPlayerId is required");if(requestId==null)throw new IllegalArgumentException("requestId is required");}
    private LocalDate gameDate(){return clock.instant().atZone(ZoneOffset.UTC).toLocalDate();}

    public record CompetitiveBoard(String gameDate,String resetPolicy,CompetitiveAttemptService.AttemptState arenaAttempts,
                                   CompetitiveAttemptService.AttemptState shadowArenaAttempts,
                                   ProductionArenaService.ArenaState arenaState,
                                   ProductionShadowArenaService.ShadowArenaState shadowArenaState){}
    public record LeaderboardEntry(UUID playerId,String displayName,long rating,long rank){}
}
