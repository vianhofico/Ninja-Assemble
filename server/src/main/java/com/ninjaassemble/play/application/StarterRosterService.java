package com.ninjaassemble.play.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StarterRosterService {
    public static final List<String> STARTERS = List.of(
            "naruto-uzumaki", "sasuke-uchiha", "sakura-haruno", "kakashi-hatake", "iruka-umino");

    private final PlayerService players;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final EnergyService energy;

    public StarterRosterService(PlayerService players, HeroOwnershipService ownership, WalletService wallet, EnergyService energy) {
        this.players = players;
        this.ownership = ownership;
        this.wallet = wallet;
        this.energy = energy;
    }

    @Transactional
    public BootstrapResult bootstrap(UUID playerId) {
        players.require(playerId);
        for (String starter : STARTERS) ownership.grantBase(playerId, starter);
        wallet.mutate(playerId, Currency.GOLD, 10_000, "STARTER_BOOTSTRAP", "vertical-slice", "bootstrap:gold");
        wallet.mutate(playerId, Currency.DIAMOND, 2_000, "STARTER_BOOTSTRAP", "vertical-slice", "bootstrap:diamond");
        var energyState = energy.refresh(playerId);
        return new BootstrapResult(ownership.list(playerId), wallet.getBalance(playerId, Currency.GOLD),
                wallet.getBalance(playerId, Currency.DIAMOND), energyState.current(), energyState.cap());
    }

    public record BootstrapResult(List<OwnedHeroView> heroes, long gold, long diamond, int energy, int energyCap) {}
}
