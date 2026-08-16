package com.ninjaassemble.play.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.catalog.HeroVersionAcquisitionCatalogService;
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
    private final HeroVersionAcquisitionCatalogService acquisitionCatalog;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final ActionRequestService requests;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SummonEngine engine = new SummonEngine();
    private final DuplicateConversionProfile duplicateProfile = new DuplicateConversionProfile(
            "duplicate-experimental-v1", Map.of(SummonRarity.R, 5L, SummonRarity.SR, 10L, SummonRarity.SSR, 30L, SummonRarity.UR, 100L));

    public SummonApplicationService(PlayerService players,
                                    CompleteRosterBannerFactory bannerFactory,
                                    HeroVersionAcquisitionCatalogService acquisitionCatalog,
                                    HeroOwnershipService ownership,
                                    WalletService wallet,
                                    ActionRequestService requests,
                                    JdbcTemplate jdbc,
                                    Clock clock) {
        this.players = players;
        this.bannerFactory = bannerFactory;
        this.acquisitionCatalog = acquisitionCatalog;
        this.ownership = ownership;
        this.wallet = wallet;
        this.requests = requests;
        this.jdbc = jdbc;
        this.clock = clock;
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

        String heroId = pulled.entry().heroId();
        var hero = acquisitionCatalog.require(heroId);
        if (!hero.summonable()) throw new IllegalStateException("summon selected non-collectible Hero Version: " + heroId);
        boolean duplicate = !ownership.grantHeroVersion(playerId, heroId).newHero();
        long duplicateCoins = duplicate ? duplicateProfile.soulsFor(pulled.entry().rarity()) : 0;
        if (duplicateCoins > 0) {
            wallet.mutate(playerId, Currency.HERO_COIN, duplicateCoins, "SUMMON_DUPLICATE", heroId,
                    "summon:" + requestId + ":duplicate");
        }

        jdbc.update("""
                insert into summon_state(player_id, banner_id, banner_version, pulls_since_pity, total_pulls)
                values (?, ?, ?, ?, 1)
                on conflict (player_id, banner_id) do update set banner_version = excluded.banner_version,
                    pulls_since_pity = excluded.pulls_since_pity, total_pulls = summon_state.total_pulls + 1
                """, playerId, banner.id(), banner.version(), pulled.nextState().pullsSincePity());
        jdbc.update("""
                insert into summon_history(id, player_id, banner_id, banner_version, seed, hero_version_id, rarity, pity_triggered, duplicate, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), playerId, banner.id(), banner.version(), seed, heroId,
                pulled.entry().rarity().name(), pulled.pityTriggered(), duplicate, clock.instant());

        SummonResult result = new SummonResult(
                heroId, hero.characterId(), hero.displayNameEn(), pulled.entry().rarity(), pulled.pityTriggered(), duplicate,
                duplicateCoins, pulled.nextState().pullsSincePity(), banner.version(), seed);
        requests.complete(playerId, requestId, encode(result));
        return result;
    }

    private static String encode(SummonResult r) {
        return String.join("\t", r.heroId(), r.characterId(), r.displayName(), r.rarity().name(),
                Boolean.toString(r.pityTriggered()), Boolean.toString(r.duplicate()), Long.toString(r.duplicateHeroCoins()),
                Integer.toString(r.pullsSincePity()), r.bannerVersion(), Long.toString(r.seed()));
    }

    private static SummonResult decode(String value) {
        String[] p = value.split("\t", -1);
        if (p.length != 10) throw new IllegalStateException("corrupt stored summon response");
        return new SummonResult(p[0], p[1], p[2], SummonRarity.valueOf(p[3]), Boolean.parseBoolean(p[4]),
                Boolean.parseBoolean(p[5]), Long.parseLong(p[6]), Integer.parseInt(p[7]), p[8], Long.parseLong(p[9]));
    }

    public record SummonResult(
            String heroId,
            String characterId,
            String displayName,
            SummonRarity rarity,
            boolean pityTriggered,
            boolean duplicate,
            long duplicateHeroCoins,
            int pullsSincePity,
            String bannerVersion,
            long seed
    ) {}
}
