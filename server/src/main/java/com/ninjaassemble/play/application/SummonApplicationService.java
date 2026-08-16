package com.ninjaassemble.play.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.summon.domain.DuplicateConversionProfile;
import com.ninjaassemble.summon.domain.SummonEngine;
import com.ninjaassemble.summon.domain.SummonRarity;
import com.ninjaassemble.summon.domain.SummonState;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SummonApplicationService {
    private static final String ACTION = "SUMMON";
    private final PlayerService players;
    private final CompleteRosterBannerFactory bannerFactory;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final ActionRequestService requests;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SummonEngine engine = new SummonEngine();
    private final DuplicateConversionProfile duplicateProfile = new DuplicateConversionProfile(
            "duplicate-experimental-v1", Map.of(SummonRarity.R, 5L, SummonRarity.SR, 10L, SummonRarity.SSR, 30L, SummonRarity.UR, 100L));

    public SummonApplicationService(PlayerService players, CompleteRosterBannerFactory bannerFactory, HeroOwnershipService ownership,
                                    WalletService wallet, ActionRequestService requests, JdbcTemplate jdbc, Clock clock) {
        this.players = players; this.bannerFactory = bannerFactory; this.ownership = ownership; this.wallet = wallet;
        this.requests = requests; this.jdbc = jdbc; this.clock = clock;
    }

    @Transactional
    public SummonResult summon(UUID playerId, UUID requestId) {
        players.require(playerId);
        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) return decode(existing.get());
        requests.reserve(playerId, requestId, ACTION);

        var banner = bannerFactory.create();
        wallet.mutate(playerId, Currency.DIAMOND, -banner.singleCost(), "SUMMON_COST", banner.id(), "summon:" + requestId + ":cost");
        int pity = jdbc.query("select pulls_since_pity from summon_state where player_id = ? and banner_id = ?",
                (rs, row) -> rs.getInt(1), playerId, banner.id()).stream().findFirst().orElse(0);
        long seed = secureRandom.nextLong();
        var pulled = engine.pull(banner, new SummonState(pity), seed);
        String[] parts = pulled.entry().heroVariantId().split("::", 2);
        String characterId = parts[0];
        String variant = parts.length == 2 ? parts[1] : "BASE";
        boolean duplicate;
        if (variant.equals("BASE")) {
            duplicate = !ownership.grantBase(playerId, characterId).newHero();
        } else {
            ownership.grantBase(playerId, characterId);
            duplicate = !ownership.unlockVariant(playerId, characterId, variant);
        }
        long duplicateCoins = duplicate ? duplicateProfile.soulsFor(pulled.entry().rarity()) : 0;
        if (duplicateCoins > 0) wallet.mutate(playerId, Currency.HERO_COIN, duplicateCoins, "SUMMON_DUPLICATE", characterId, "summon:" + requestId + ":duplicate");

        jdbc.update("""
                insert into summon_state(player_id, banner_id, banner_version, pulls_since_pity, total_pulls)
                values (?, ?, ?, ?, 1)
                on conflict (player_id, banner_id) do update set banner_version = excluded.banner_version,
                    pulls_since_pity = excluded.pulls_since_pity, total_pulls = summon_state.total_pulls + 1
                """, playerId, banner.id(), banner.version(), pulled.nextState().pullsSincePity());
        jdbc.update("""
                insert into summon_history(id, player_id, banner_id, banner_version, seed, hero_variant_id, rarity, pity_triggered, duplicate, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), playerId, banner.id(), banner.version(), seed, pulled.entry().heroVariantId(), pulled.entry().rarity().name(), pulled.pityTriggered(), duplicate, clock.instant());
        SummonResult result = new SummonResult(characterId, variant, pulled.entry().rarity(), pulled.pityTriggered(), duplicate, duplicateCoins,
                pulled.nextState().pullsSincePity(), banner.version(), seed);
        requests.complete(playerId, requestId, encode(result));
        return result;
    }

    private static String encode(SummonResult r) {
        return String.join("\t", r.characterId(), r.variant(), r.rarity().name(), Boolean.toString(r.pityTriggered()),
                Boolean.toString(r.duplicate()), Long.toString(r.duplicateHeroCoins()), Integer.toString(r.pullsSincePity()), r.bannerVersion(), Long.toString(r.seed()));
    }

    private static SummonResult decode(String value) {
        String[] p = value.split("\t", -1);
        if (p.length != 9) throw new IllegalStateException("corrupt stored summon response");
        return new SummonResult(p[0], p[1], SummonRarity.valueOf(p[2]), Boolean.parseBoolean(p[3]), Boolean.parseBoolean(p[4]),
                Long.parseLong(p[5]), Integer.parseInt(p[6]), p[7], Long.parseLong(p[8]));
    }

    public record SummonResult(String characterId, String variant, SummonRarity rarity, boolean pityTriggered, boolean duplicate,
                               long duplicateHeroCoins, int pullsSincePity, String bannerVersion, long seed) {}
}
