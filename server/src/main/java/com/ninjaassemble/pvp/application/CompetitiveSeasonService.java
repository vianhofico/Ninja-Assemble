package com.ninjaassemble.pvp.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monthly UTC season rollover and idempotent previous-season reward settlement. */
@Service
public final class CompetitiveSeasonService {
    public static final long INITIAL_RATING = 1_000L;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ROOT);

    private final JdbcTemplate jdbc;
    private final WalletService wallet;
    private final Clock clock;

    public CompetitiveSeasonService(JdbcTemplate jdbc, WalletService wallet, Clock clock) {
        this.jdbc = jdbc;
        this.wallet = wallet;
        this.clock = clock;
    }

    public String currentSeasonId(Mode mode) { return prefix(mode) + currentMonth().format(MONTH); }
    public String previousSeasonId(Mode mode) { return prefix(mode) + currentMonth().minusMonths(1).format(MONTH); }
    public Instant currentSeasonEndsAt() { return currentMonth().plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(); }

    @Transactional
    public long ensureArenaProfile(UUID playerId) {
        String current = currentSeasonId(Mode.ARENA);
        List<ArenaProfileRow> rows = jdbc.query(
                "select season_id, rating from arena_profiles where player_id=? for update",
                (rs, row) -> new ArenaProfileRow(rs.getString("season_id"), rs.getLong("rating")), playerId);
        if (rows.isEmpty()) {
            jdbc.update("insert into arena_profiles(player_id, season_id, rating, updated_at) values (?, ?, ?, ?)",
                    playerId, current, INITIAL_RATING, clock.instant());
            return INITIAL_RATING;
        }
        ArenaProfileRow existing = rows.get(0);
        if (!current.equals(existing.seasonId())) {
            settle(playerId, Mode.ARENA, existing.seasonId(), existing.rating());
            jdbc.update("update arena_profiles set season_id=?, rating=?, updated_at=? where player_id=?",
                    current, INITIAL_RATING, clock.instant(), playerId);
            return INITIAL_RATING;
        }
        return existing.rating();
    }

    @Transactional
    public long ensureShadowProfile(UUID playerId, String rosterSnapshot) {
        String current = currentSeasonId(Mode.SHADOW_ARENA);
        jdbc.update("""
                insert into shadow_arena_profiles(player_id, season_id, rating, roster_snapshot, updated_at)
                values (?, ?, ?, cast(? as jsonb), ?)
                on conflict (player_id, season_id)
                do update set roster_snapshot=excluded.roster_snapshot, updated_at=excluded.updated_at
                """, playerId, current, INITIAL_RATING, rosterSnapshot == null ? "{}" : rosterSnapshot, clock.instant());
        Long rating = jdbc.queryForObject(
                "select rating from shadow_arena_profiles where player_id=? and season_id=?",
                Long.class, playerId, current);
        return rating == null ? INITIAL_RATING : rating;
    }

    @Transactional
    public SeasonRewardState previousReward(UUID playerId, Mode mode) {
        String previous = previousSeasonId(mode);
        settlePreviousIfPresent(playerId, mode, previous);
        List<SeasonResultRow> rows = jdbc.query("""
                select final_rating, reward_amount, claimed
                from competitive_season_results where player_id=? and mode=? and season_id=?
                """, (rs, row) -> new SeasonResultRow(rs.getLong("final_rating"), rs.getLong("reward_amount"), rs.getBoolean("claimed")),
                playerId, mode.name(), previous);
        if (rows.isEmpty()) return new SeasonRewardState(previous, 0L, 0L, false, false);
        SeasonResultRow row = rows.get(0);
        return new SeasonRewardState(previous, row.finalRating(), row.rewardAmount(), !row.claimed() && row.rewardAmount() > 0, row.claimed());
    }

    @Transactional
    public SeasonRewardState claimPrevious(UUID playerId, Mode mode) {
        long lockKey = playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits() ^ mode.ordinal();
        jdbc.query("select pg_advisory_xact_lock(?)", rs -> { }, lockKey);
        SeasonRewardState state = previousReward(playerId, mode);
        if (state.claimed() || !state.claimable()) return state;
        Currency currency = mode == Mode.ARENA ? Currency.ARENA_COIN : Currency.SHADOW_COIN;
        String key = "competitive-season:" + mode.name() + ":" + state.seasonId() + ":" + playerId;
        wallet.mutate(playerId, currency, state.rewardAmount(), "COMPETITIVE_SEASON_REWARD", state.seasonId(), key);
        jdbc.update("""
                update competitive_season_results set claimed=true, claimed_at=?
                where player_id=? and mode=? and season_id=? and claimed=false
                """, clock.instant(), playerId, mode.name(), state.seasonId());
        return new SeasonRewardState(state.seasonId(), state.finalRating(), state.rewardAmount(), false, true);
    }

    private void settlePreviousIfPresent(UUID playerId, Mode mode, String seasonId) {
        Integer exists = jdbc.queryForObject("select count(*) from competitive_season_results where player_id=? and mode=? and season_id=?",
                Integer.class, playerId, mode.name(), seasonId);
        if (exists != null && exists > 0) return;
        if (mode == Mode.ARENA) {
            List<Long> ratings = jdbc.query("select rating from arena_profiles where player_id=? and season_id=?",
                    (rs, row) -> rs.getLong(1), playerId, seasonId);
            if (!ratings.isEmpty()) settle(playerId, mode, seasonId, ratings.get(0));
        } else {
            List<Long> ratings = jdbc.query("select rating from shadow_arena_profiles where player_id=? and season_id=?",
                    (rs, row) -> rs.getLong(1), playerId, seasonId);
            if (!ratings.isEmpty()) settle(playerId, mode, seasonId, ratings.get(0));
        }
    }

    private void settle(UUID playerId, Mode mode, String seasonId, long finalRating) {
        long reward = rewardFor(mode, finalRating);
        jdbc.update("""
                insert into competitive_season_results(player_id, mode, season_id, final_rating, reward_amount, claimed, settled_at)
                values (?, ?, ?, ?, ?, false, ?)
                on conflict (player_id, mode, season_id) do nothing
                """, playerId, mode.name(), seasonId, finalRating, reward, clock.instant());
    }

    public static long rewardFor(Mode mode, long rating) {
        if (mode == Mode.ARENA) {
            if (rating >= 1_600) return 500;
            if (rating >= 1_300) return 300;
            if (rating >= 1_000) return 150;
            return 75;
        }
        if (rating >= 1_600) return 800;
        if (rating >= 1_300) return 500;
        if (rating >= 1_000) return 250;
        return 100;
    }

    private YearMonth currentMonth() { return YearMonth.from(clock.instant().atZone(ZoneOffset.UTC)); }
    private static String prefix(Mode mode) { return mode == Mode.ARENA ? "arena-" : "shadow-"; }

    public enum Mode { ARENA, SHADOW_ARENA }
    public record SeasonRewardState(String seasonId, long finalRating, long rewardAmount, boolean claimable, boolean claimed) { }
    private record ArenaProfileRow(String seasonId, long rating) { }
    private record SeasonResultRow(long finalRating, long rewardAmount, boolean claimed) { }
}
