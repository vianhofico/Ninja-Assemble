package com.ninjaassemble.player.api;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import com.ninjaassemble.security.SessionTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {
    private final PlayerService players;
    private final EnergyService energy;
    private final WalletService wallet;
    private final SessionTokenService sessions;

    public PlayerController(PlayerService players, EnergyService energy, WalletService wallet, SessionTokenService sessions) {
        this.players = players; this.energy = energy; this.wallet = wallet; this.sessions = sessions;
    }

    @PostMapping("/guest")
    public PlayerView guest(@Valid @RequestBody GuestLogin request) {
        return view(players.loginOrCreateGuest(request.guestKey(), request.displayName()));
    }

    @GetMapping("/{id}")
    public PlayerView get(@PathVariable UUID id) { return view(players.require(id)); }

    @GetMapping("/{id}/state")
    public Map<String, Object> state(@PathVariable UUID id) {
        return Map.of("player", view(players.require(id)), "energy", energy.refresh(id), "wallet", wallet.snapshot(id));
    }

    private PlayerView view(PlayerEntity p) {
        return new PlayerView(p.getId(), p.getDisplayName(), p.getAccountLevel(), p.getAccountExp(), sessions.issue(p.getId()));
    }

    public record GuestLogin(@NotBlank String guestKey, String displayName) {}
    public record PlayerView(UUID id, String displayName, int level, long exp, String sessionToken) {}
}
