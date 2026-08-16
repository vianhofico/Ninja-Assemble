package com.ninjaassemble.campaign.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CampaignProgressService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CampaignProgressService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Map<String, StageProgress> load(UUID playerId) {
        Map<String, StageProgress> result = new LinkedHashMap<>();
        jdbc.query("""
                select stage_id, clear_count, best_stars, first_cleared_at, last_cleared_at
                from campaign_stage_progress where player_id = ?
                """, rs -> {
            Timestamp first = rs.getTimestamp("first_cleared_at");
            Timestamp last = rs.getTimestamp("last_cleared_at");
            StageProgress progress = new StageProgress(
                    rs.getString("stage_id"), rs.getInt("clear_count"), rs.getInt("best_stars"),
                    first == null ? null : first.toInstant(), last == null ? null : last.toInstant());
            result.put(progress.stageId(), progress);
        }, playerId);
        return Map.copyOf(result);
    }

    @Transactional(readOnly = true)
    public Set<String> completedStageIds(UUID playerId) {
        return load(playerId).values().stream().filter(it -> it.clearCount() > 0).map(StageProgress::stageId).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Transactional
    public ClearRecord recordClear(UUID playerId, String stageId, int stars) {
        if (stars < 1 || stars > 3) throw new IllegalArgumentException("clear stars must be 1..3");
        Instant now = clock.instant();
        int inserted = jdbc.update("""
                insert into campaign_stage_progress(player_id, stage_id, clear_count, best_stars, first_cleared_at, last_cleared_at)
                values (?, ?, 1, ?, ?, ?)
                on conflict (player_id, stage_id) do nothing
                """, playerId, stageId, stars, now, now);
        boolean firstClear = inserted == 1;
        if (!firstClear) {
            jdbc.update("""
                    update campaign_stage_progress
                    set clear_count = clear_count + 1,
                        best_stars = greatest(best_stars, ?),
                        first_cleared_at = coalesce(first_cleared_at, ?),
                        last_cleared_at = ?
                    where player_id = ? and stage_id = ?
                    """, stars, now, now, playerId, stageId);
        }
        StageProgress progress = load(playerId).get(stageId);
        if (progress == null) throw new IllegalStateException("campaign progress write did not persist: " + stageId);
        return new ClearRecord(firstClear, progress);
    }

    public record StageProgress(String stageId, int clearCount, int bestStars, Instant firstClearedAt, Instant lastClearedAt) {}
    public record ClearRecord(boolean firstClear, StageProgress progress) {}
}
