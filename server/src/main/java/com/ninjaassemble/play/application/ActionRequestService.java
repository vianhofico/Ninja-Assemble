package com.ninjaassemble.play.application;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActionRequestService {
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ActionRequestService(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public Optional<String> existing(UUID playerId, UUID requestId, String actionType) {
        return jdbc.query("select action_type, result_text from play_action_log where player_id = ? and request_key = ?",
                (rs, row) -> new Existing(rs.getString(1), rs.getString(2)), playerId, requestId.toString()).stream().findFirst().map(value -> {
                    if (!value.actionType().equals(actionType)) throw new IllegalStateException("request id already used for another action");
                    if (value.resultText() == null) throw new IllegalStateException("request is still in progress");
                    return value.resultText();
                });
    }

    public void reserve(UUID playerId, UUID requestId, String actionType) {
        int rows = jdbc.update("""
                insert into play_action_log(player_id, request_key, action_type)
                values (?, ?, ?)
                on conflict (player_id, request_key) do nothing
                """, playerId, requestId.toString(), actionType);
        if (rows == 0) throw new IllegalStateException("duplicate request reservation");
    }

    public void complete(UUID playerId, UUID requestId, String resultText) {
        jdbc.update("update play_action_log set result_text = ?, completed_at = ? where player_id = ? and request_key = ?",
                resultText, clock.instant(), playerId, requestId.toString());
    }

    private record Existing(String actionType, String resultText) {}
}
