package com.ninjaassemble.play.api;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.player.domain.PlayerEntity;
import com.ninjaassemble.player.domain.PlayerRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dev/{playerId}")
@ConditionalOnProperty(name = "game.dev.enabled", havingValue = "true")
public class DevPlaytestController {
    private static final long DEV_GOLD = 1_000_000L;
    private static final long DEV_DIAMOND = 10_000L;

    private final WalletService wallet;
    private final HeroOwnershipService ownership;
    private final HeroCatalogService catalog;
    private final PlayerRepository players;
    private final Clock clock;

    public DevPlaytestController(WalletService wallet, HeroOwnershipService ownership, HeroCatalogService catalog,
                                 PlayerRepository players, Clock clock) {
        this.wallet = wallet;
        this.ownership = ownership;
        this.catalog = catalog;
        this.players = players;
        this.clock = clock;
    }

    @PostMapping("/grant-standard-pack")
    @Transactional
    public DevState grantStandardPack(@PathVariable UUID playerId) {
        wallet.mutate(playerId, Currency.GOLD, DEV_GOLD, "PLAYTEST_DEV_GRANT", "DEV_MENU", null);
        wallet.mutate(playerId, Currency.DIAMOND, DEV_DIAMOND, "PLAYTEST_DEV_GRANT", "DEV_MENU", null);
        return state(playerId);
    }

    @PostMapping("/unlock-all-heroes")
    @Transactional
    public DevRosterResult unlockAllHeroes(@PathVariable UUID playerId) {
        int newlyGranted = 0;
        for (var entry : catalog.all()) {
            if (ownership.grantBase(playerId, entry.id()).newHero()) newlyGranted++;
        }
        return new DevRosterResult(newlyGranted, ownership.list(playerId).size());
    }

    @PostMapping("/refill-energy")
    @Transactional
    public DevState refillEnergy(@PathVariable UUID playerId) {
        PlayerEntity player = requirePlayer(playerId);
        player.setEnergyState(player.getEnergyCap(), clock.instant());
        players.save(player);
        return state(playerId);
    }

    private DevState state(UUID playerId) {
        PlayerEntity player = requirePlayer(playerId);
        return new DevState(
                wallet.getBalance(playerId, Currency.GOLD),
                wallet.getBalance(playerId, Currency.DIAMOND),
                player.getEnergy());
    }

    private PlayerEntity requirePlayer(UUID playerId) {
        return players.findById(playerId).orElseThrow(() -> new IllegalArgumentException("player not found"));
    }

    public record DevState(long gold, long diamond, int energy) {}
    public record DevRosterResult(int newlyGranted, int ownedHeroes) {}
}
