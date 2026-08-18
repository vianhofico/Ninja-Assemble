package com.ninjaassemble.pvp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CompetitiveApplicationService {
    public static final int ARENA_DAILY_ATTEMPTS = 5;
    public static final int SHADOW_DAILY_ATTEMPTS = 3;
    public static final String RESET_POLICY = "UTC_DAILY";

    private final ArenaApplicationService arena;
    private final ShadowArenaApplicationService shadowArena;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final Clock utcClock;

    public CompetitiveApplicationService(ArenaApplicationService arena,
                                         ShadowArenaApplicationService shadowArena,
                                         JdbcTemplate jdbc,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.arena = arena;
        this.shadowArena = shadowArena;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.utcClock = clock.withZone(ZoneOffset.UTC);
    }

    @Transactional(readOnly = true)
    public CompetitiveBoard board(UUID playerId) {
        ArenaApplicationService.ArenaState arenaState = arena.state(playerId);
        ShadowArenaApplicationService.ShadowArenaState shadowState = shadowArena.state(playerId);
        LocalDate gameDate = LocalDate.now(utcClock);
        return new CompetitiveBoard(
                gameDate.toString(), RESET_POLICY,
                modeState(playerId, Mode.ARENA, ArenaApplicationService.SEASON_ID, ARENA_DAILY_ATTEMPTS),
                modeState(playerId, Mode.SHADOW_ARENA, ShadowArenaApplicationService.SEASON_ID, SHADOW_DAILY_ATTEMPTS),
                arenaState, shadowState);
    }

    @Transactional
    public CompetitiveBattleResult fightArena(UUID playerId, UUID opponentPlayerId, UUID requestId) {
        return execute(playerId, opponentPlayerId, requestId, Mode.ARENA,
                ArenaApplicationService.SEASON_ID, ARENA_DAILY_ATTEMPTS,
                () -> arena.fight(playerId, opponentPlayerId));
    }

    @Transactional
    public CompetitiveBattleResult fightShadowArena(UUID playerId, UUID opponentPlayerId, UUID requestId) {
        return execute(playerId, opponentPlayerId, requestId, Mode.SHADOW_ARENA,
                ShadowArenaApplicationService.SEASON_ID, SHADOW_DAILY_ATTEMPTS,
                () -> shadowArena.fight(playerId, opponentPlayerId));
    }

    @Transactional(readOnly = true)
    public CompetitiveHistory history(UUID playerId, Mode mode) {
        String seasonId = season(mode);
        List<JsonNode> battles = jdbc.query("""
                select result_json::text
                from competitive_action_runs
                where player_id = ? and mode = ? and season_id = ?
                order by created_at desc
                limit 20
                """, (rs, rowNum) -> readJson(rs.getString(1)), playerId, mode.name(), seasonId);
        return new CompetitiveHistory(mode.name(), seasonId, List.copyOf(battles));
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(Mode mode) {
        String table = mode == Mode.ARENA ? "arena_profiles" : "shadow_arena_profiles";
        String seasonId = season(mode);
        return jdbc.query("""
                select ranked.player_id, p.display_name, ranked.rating, ranked.rank
                from (
                    select player_id, rating, dense_rank() over(order by rating desc, player_id) as rank
                    from %s
                    where season_id = ?
                ) ranked
                join players p on p.id = ranked.player_id
                order by ranked.rank, ranked.player_id
                limit 100
                """.formatted(table), (rs, rowNum) -> new LeaderboardEntry(
                rs.getObject("player_id", UUID.class), rs.getString("display_name"),
                rs.getLong("rating"), rs.getLong("rank")), seasonId);
    }

    private CompetitiveBattleResult execute(UUID playerId, UUID opponentPlayerId, UUID requestId,
                                            Mode mode, String seasonId, int limit, BattleCall call) {
        if (opponentPlayerId == null) throw new IllegalArgumentException("opponentPlayerId is required");
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        lock(playerId, mode);
        LocalDate gameDate = LocalDate.now(utcClock);

        List<String> existing = jdbc.query("""
                select result_json::text from competitive_action_runs
                where player_id = ? and mode = ? and request_id = ?
                """, (rs, rowNum) -> rs.getString(1), playerId, mode.name(), requestId);
        if (!existing.isEmpty()) {
            ModeState state = modeState(playerId, mode, seasonId, limit);
            return new CompetitiveBattleResult(requestId, true, state.attemptsRemaining(), readJson(existing.get(0)));
        }

        int used = attemptsUsed(playerId, mode, seasonId, gameDate);
        if (used >= limit) throw new IllegalStateException(mode.name() + " daily attempts exhausted");

        Object battle = call.run();
        JsonNode result = objectMapper.valueToTree(battle);
        jdbc.update("""
                insert into competitive_action_runs(player_id, mode, request_id, opponent_player_id, season_id, game_date, result_json)
                values (?, ?, ?, ?, ?, ?, cast(? as jsonb))
                """, playerId, mode.name(), requestId, opponentPlayerId, seasonId, gameDate, writeJson(result));
        jdbc.update("""
                insert into competitive_daily_attempts(player_id, mode, season_id, game_date, attempts_used, updated_at)
                values (?, ?, ?, ?, 1, now())
                on conflict (player_id, mode, season_id, game_date)
                do update set attempts_used = competitive_daily_attempts.attempts_used + 1, updated_at = excluded.updated_at
                """, playerId, mode.name(), seasonId, gameDate);
        return new CompetitiveBattleResult(requestId, false, Math.max(0, limit - used - 1), result);
    }

    private ModeState modeState(UUID playerId, Mode mode, String seasonId, int limit) {
        LocalDate gameDate = LocalDate.now(utcClock);
        int used = attemptsUsed(playerId, mode, seasonId, gameDate);
        Long rank = playerRank(playerId, mode, seasonId);
        return new ModeState(mode.name(), seasonId, limit, used, Math.max(0, limit - used), rank == null ? 0 : rank);
    }

    private int attemptsUsed(UUID playerId, Mode mode, String seasonId, LocalDate gameDate) {
        List<Integer> rows = jdbc.query("""
                select attempts_used from competitive_daily_attempts
                where player_id = ? and mode = ? and season_id = ? and game_date = ?
                """, (rs, rowNum) -> rs.getInt(1), playerId, mode.name(), seasonId, gameDate);
        return rows.isEmpty() ? 0 : rows.get(0);
    }

    private Long playerRank(UUID playerId, Mode mode, String seasonId) {
        String table = mode == Mode.ARENA ? "arena_profiles" : "shadow_arena_profiles";
        List<Long> rows = jdbc.query("""
                select rank from (
                    select player_id, dense_rank() over(order by rating desc, player_id) as rank
                    from %s where season_id = ?
                ) ranked where player_id = ?
                """.formatted(table), (rs, rowNum) -> rs.getLong(1), seasonId, playerId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void lock(UUID playerId, Mode mode) {
        jdbc.queryForList("select pg_advisory_xact_lock(hashtext(?))", "competitive:" + mode.name() + ":" + playerId);
    }

    private String season(Mode mode) {
        return mode == Mode.ARENA ? ArenaApplicationService.SEASON_ID : ShadowArenaApplicationService.SEASON_ID;
    }

    private JsonNode readJson(String json) {
        try { return objectMapper.readTree(json); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("invalid stored competitive result", exception); }
    }

    private String writeJson(JsonNode json) {
        try { return objectMapper.writeValueAsString(json); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("cannot persist competitive result", exception); }
    }

    @FunctionalInterface private interface BattleCall { Object run(); }

    public enum Mode { ARENA, SHADOW_ARENA }
    public record ModeState(String mode, String seasonId, int dailyAttemptLimit, int attemptsUsed, int attemptsRemaining, long rank) {}
    public record CompetitiveBoard(String gameDate, String resetPolicy, ModeState arena, ModeState shadowArena,
                                   ArenaApplicationService.ArenaState arenaState,
                                   ShadowArenaApplicationService.ShadowArenaState shadowArenaState) {}
    public record CompetitiveBattleResult(UUID requestId, boolean replayed, int attemptsRemaining, JsonNode battle) {}
    public record CompetitiveHistory(String mode, String seasonId, List<JsonNode> battles) {}
    public record LeaderboardEntry(UUID playerId, String displayName, long rating, long rank) {}
}
