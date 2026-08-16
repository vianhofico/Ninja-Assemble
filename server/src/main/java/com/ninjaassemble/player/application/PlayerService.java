package com.ninjaassemble.player.application;

import com.ninjaassemble.player.domain.PlayerEntity;
import com.ninjaassemble.player.domain.PlayerRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerService {
    private final PlayerRepository players;
    private final Clock clock;
    private final int energyCap;

    public PlayerService(PlayerRepository players, Clock clock, @Value("${game.energy.cap:120}") int energyCap) {
        this.players = players;
        this.clock = clock;
        this.energyCap = energyCap;
    }

    @Transactional
    public PlayerEntity loginOrCreateGuest(String guestKey, String displayName) {
        if (guestKey == null || guestKey.isBlank()) throw new IllegalArgumentException("guestKey is required");
        return players.findByGuestKey(guestKey).orElseGet(() -> {
            Instant now = clock.instant();
            String safeName = displayName == null || displayName.isBlank() ? "Ninja" : displayName.trim();
            return players.save(new PlayerEntity(UUID.randomUUID(), safeName, guestKey.trim(), energyCap, now));
        });
    }

    @Transactional(readOnly = true)
    public PlayerEntity require(UUID playerId) {
        return players.findById(playerId).orElseThrow(() -> new IllegalArgumentException("player not found"));
    }
}
