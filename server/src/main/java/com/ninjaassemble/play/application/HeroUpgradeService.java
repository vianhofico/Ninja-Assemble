package com.ninjaassemble.play.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.PlayerService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeroUpgradeService {
    private static final String ACTION = "HERO_LEVEL_UP";
    private static final int MAX_EXPERIMENTAL_LEVEL = 100;
    private final PlayerService players;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final ActionRequestService requests;
    private final JdbcTemplate jdbc;

    public HeroUpgradeService(PlayerService players, HeroOwnershipService ownership, WalletService wallet, ActionRequestService requests, JdbcTemplate jdbc) {
        this.players = players; this.ownership = ownership; this.wallet = wallet; this.requests = requests; this.jdbc = jdbc;
    }

    @Transactional
    public UpgradeResult levelUp(UUID playerId, UUID playerHeroId, UUID requestId) {
        players.require(playerId);
        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) return decode(playerId, existing.get());
        requests.reserve(playerId, requestId, ACTION);
        OwnedHeroView before = ownership.requireOwned(playerId, playerHeroId);
        if (before.level() >= MAX_EXPERIMENTAL_LEVEL) throw new IllegalStateException("hero is at the current experimental level cap");
        long goldCost = before.level() * 100L;
        wallet.mutate(playerId, Currency.GOLD, -goldCost, "HERO_LEVEL_UP", playerHeroId.toString(), "upgrade:" + requestId + ":gold");
        jdbc.update("update player_heroes set level = level + 1 where id = ? and player_id = ?", playerHeroId, playerId);
        OwnedHeroView after = ownership.requireOwned(playerId, playerHeroId);
        UpgradeResult result = new UpgradeResult(after, goldCost, "experimental-level-cost-v1");
        requests.complete(playerId, requestId, after.id() + "\t" + goldCost);
        return result;
    }

    private UpgradeResult decode(UUID playerId, String stored) {
        String[] p = stored.split("\t", -1);
        if (p.length != 2) throw new IllegalStateException("corrupt stored upgrade response");
        UUID heroId = UUID.fromString(p[0]);
        long cost = Long.parseLong(p[1]);
        return new UpgradeResult(ownership.requireOwned(playerId, heroId), cost, "experimental-level-cost-v1");
    }

    public record UpgradeResult(OwnedHeroView hero, long goldCost, String costProfileVersion) {}
}
