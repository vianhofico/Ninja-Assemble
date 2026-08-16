package com.ninjaassemble.player.application;

import com.ninjaassemble.player.domain.PlayerEntity;
import com.ninjaassemble.player.domain.PlayerRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnergyService {
    private final PlayerRepository players;
    private final Clock clock;
    private final long regenSeconds;

    public EnergyService(PlayerRepository players, Clock clock, @Value("${game.energy.regen-seconds:300}") long regenSeconds) {
        this.players = players; this.clock = clock; this.regenSeconds = regenSeconds;
    }

    @Transactional
    public EnergySnapshot refresh(UUID playerId) {
        PlayerEntity player = players.findById(playerId).orElseThrow(() -> new IllegalArgumentException("player not found"));
        Instant now = clock.instant();
        if (player.getEnergy() >= player.getEnergyCap()) return new EnergySnapshot(player.getEnergy(), player.getEnergyCap(), null);
        long elapsed = Math.max(0, Duration.between(player.getEnergyUpdatedAt(), now).getSeconds());
        long gained = elapsed / regenSeconds;
        if (gained > 0) {
            int newEnergy = (int) Math.min(player.getEnergyCap(), player.getEnergy() + gained);
            Instant anchor = player.getEnergyUpdatedAt().plusSeconds(gained * regenSeconds);
            player.setEnergyState(newEnergy, newEnergy == player.getEnergyCap() ? now : anchor);
        }
        Instant next = player.getEnergy() >= player.getEnergyCap() ? null : player.getEnergyUpdatedAt().plusSeconds(regenSeconds);
        return new EnergySnapshot(player.getEnergy(), player.getEnergyCap(), next);
    }

    @Transactional
    public EnergySnapshot spend(UUID playerId, int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        refresh(playerId);
        PlayerEntity player = players.findById(playerId).orElseThrow();
        if (player.getEnergy() < amount) throw new IllegalStateException("not enough energy");
        player.setEnergyState(player.getEnergy() - amount, clock.instant());
        return new EnergySnapshot(player.getEnergy(), player.getEnergyCap(), player.getEnergyUpdatedAt().plusSeconds(regenSeconds));
    }

    public record EnergySnapshot(int current, int cap, Instant nextEnergyAt) {}
}
