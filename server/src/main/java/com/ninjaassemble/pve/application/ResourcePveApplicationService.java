package com.ninjaassemble.pve.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninjaassemble.battle.sim.BattleOutcome;
import com.ninjaassemble.battle.sim.BattleResult;
import com.ninjaassemble.battle.sim.BattleRuleset;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.RealtimeBattleEngine;
import com.ninjaassemble.battle.sim.RealtimeBattleRequest;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.catalog.HeroCatalogEntry;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.inventory.application.InventoryService;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import com.ninjaassemble.play.application.FormationService;
import com.ninjaassemble.play.domain.BattleParticipant;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class ResourcePveApplicationService {
    public static final String RULESET_VERSION = "resource-pve-realtime-v1";
    private final ResourcePveCatalogService catalog;
    private final PlayerService players;
    private final FormationService formations;
    private final ExperimentalCombatStatsResolver stats;
    private final HeroCatalogService heroes;
    private final EnergyService energy;
    private final WalletService wallet;
    private final InventoryService inventory;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final RealtimeBattleEngine engine = new RealtimeBattleEngine();

    public ResourcePveApplicationService(ResourcePveCatalogService catalog, PlayerService players,
                                         FormationService formations, ExperimentalCombatStatsResolver stats,
                                         HeroCatalogService heroes, EnergyService energy, WalletService wallet,
                                         InventoryService inventory, JdbcTemplate jdbc, Clock clock, ObjectMapper objectMapper) {
        this.catalog = catalog; this.players = players; this.formations = formations; this.stats = stats;
        this.heroes = heroes; this.energy = energy; this.wallet = wallet; this.inventory = inventory;
        this.jdbc = jdbc; this.clock = clock; this.objectMapper = objectMapper;
    }

    @Transactional
    public ResourcePveBoard board(UUID playerId) {
        PlayerEntity player = players.require(playerId);
        LocalDate date = gameDate();
        EnergyService.EnergySnapshot energyState = energy.refresh(playerId);
        List<ResourcePveModeView> modes = catalog.all().stream().map(entry -> {
            Progress progress = progress(playerId, entry.definition().id(), date);
            int limit = entry.definition().dailyAttemptLimit() == null ? Integer.MAX_VALUE : entry.definition().dailyAttemptLimit();
            boolean levelReady = player.getAccountLevel() >= entry.minPlayerLevel();
            boolean energyReady = energyState.current() >= entry.definition().energyCost();
            boolean attemptsReady = progress.attempts < limit;
            String blocked = !levelReady ? "PLAYER_LEVEL" : !energyReady ? "ENERGY" : !attemptsReady ? "DAILY_ATTEMPTS" : "";
            return new ResourcePveModeView(entry.definition().id(), entry.definition().type().name(), entry.nameEn(), entry.nameVi(),
                    entry.definition().teamSize(), entry.definition().energyCost(), limit, Math.max(0, limit - progress.attempts),
                    entry.minPlayerLevel(), progress.clears, progress.bestScore, blocked.isEmpty(), blocked,
                    entry.rewardGold(), entry.rewardItemId(), entry.rewardItemQuantity(), entry.resetPolicy());
        }).toList();
        return new ResourcePveBoard(ResourcePveCatalogService.VERSION, RULESET_VERSION, date.toString(),
                player.getAccountLevel(), energyState.current(), energyState.cap(), modes);
    }

    @Transactional
    public ResourcePveBattleView play(UUID playerId, String modeId, UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        if (modeId == null || modeId.isBlank()) throw new IllegalArgumentException("modeId is required");
        long lockKey = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits();
        jdbc.query("select pg_advisory_xact_lock(?)", rs -> { }, lockKey);
        ResourcePveBattleView replay = loadRun(requestId);
        if (replay != null) {
            if (!playerId.toString().equals(replay.playerId()) || !modeId.equals(replay.modeId()))
                throw new IllegalStateException("resource PvE requestId already belongs to another player/mode");
            return replay.withReplayed(true);
        }

        ResourcePveCatalogService.ModeEntry mode = catalog.require(modeId);
        PlayerEntity player = players.require(playerId);
        if (player.getAccountLevel() < mode.minPlayerLevel()) throw new IllegalStateException("resource PvE player level requirement not met");
        LocalDate date = gameDate();
        Progress current = progress(playerId, modeId, date);
        int limit = mode.definition().dailyAttemptLimit() == null ? Integer.MAX_VALUE : mode.definition().dailyAttemptLimit();
        if (current.attempts >= limit) throw new IllegalStateException("resource PvE daily attempt limit reached");
        FormationService.FormationView formation = formations.load(playerId);
        if (formation.heroes().size() != mode.definition().teamSize())
            throw new IllegalStateException("resource PvE requires a five-ninja formation");
        EnergyService.EnergySnapshot energyAfter = energy.spend(playerId, mode.definition().energyCost());

        List<BattleUnitSeed> units = new ArrayList<>();
        List<BattleParticipant> participants = new ArrayList<>();
        for (int slot = 0; slot < formation.heroes().size(); slot++) {
            OwnedHeroView hero = formation.heroes().get(slot);
            BattleUnitSeed unit = stats.resolve(hero.id().toString(), hero.heroId(), hero.awakened(), hero.level(), TeamSide.A, slot);
            units.add(unit);
            participants.add(BattleParticipant.heroVersion(unit.id(), hero.characterId(), hero.heroId(), hero.awakened(),
                    hero.awakeningId(), hero.displayName(), hero.level(), unit.side(), unit.slot(), unit.maxHp()));
        }
        for (ResourcePveCatalogService.EnemySpec enemy : mode.enemies()) {
            HeroCatalogEntry hero = heroes.require(enemy.characterId());
            BattleUnitSeed unit = stats.resolve("pve:" + modeId + ":s" + enemy.slot(), enemy.characterId(), enemy.variant(), enemy.level(), TeamSide.B, enemy.slot());
            units.add(unit);
            participants.add(new BattleParticipant(unit.id(), enemy.characterId(), hero.character(), enemy.variant(), enemy.level(), unit.side(), unit.slot(), unit.maxHp()));
        }

        long seed = new java.security.SecureRandom().nextLong();
        BattleResult battle = engine.simulate(new RealtimeBattleRequest(seed, BattleRuleset.experimentalV1(), units));
        boolean won = battle.outcome() == BattleOutcome.TEAM_A;
        long gold = 0L; String itemId = null; long itemQuantity = 0L;
        if (won) {
            gold = mode.rewardGold();
            if (gold > 0) wallet.mutate(playerId, Currency.GOLD, gold, "RESOURCE_PVE_REWARD", modeId, "resource-pve:" + requestId + ":GOLD");
            if (mode.rewardItemId() != null && mode.rewardItemQuantity() > 0) {
                inventory.mutate(playerId, mode.rewardItemId(), mode.rewardItemQuantity(), "RESOURCE_PVE_REWARD", "resource-pve:" + requestId + ":item:" + mode.rewardItemId());
                itemId = mode.rewardItemId(); itemQuantity = mode.rewardItemQuantity();
            }
        }

        int clearsDelta = won ? 1 : 0;
        long score = won ? Math.max(1L, 120_000L - battle.durationMs()) : 0L;
        jdbc.update("""
                insert into pve_mode_progress(player_id, mode_id, game_date, attempts, clears, best_score)
                values (?, ?, ?, 1, ?, ?)
                on conflict (player_id, mode_id, game_date) do update
                set attempts = pve_mode_progress.attempts + 1,
                    clears = pve_mode_progress.clears + excluded.clears,
                    best_score = greatest(coalesce(pve_mode_progress.best_score, 0), excluded.best_score)
                """, playerId, modeId, date, clearsDelta, score);

        ResourcePveBattleView result = new ResourcePveBattleView(requestId.toString(), playerId.toString(), modeId,
                ResourcePveCatalogService.VERSION, RULESET_VERSION, date.toString(), false, mode.definition().energyCost(),
                energyAfter.current(), won, gold, itemId, itemQuantity, seed, List.copyOf(participants), battle);
        jdbc.update("insert into resource_pve_runs(request_id, player_id, mode_id, game_date, result_json) values (?, ?, ?, ?, ?)",
                requestId, playerId, modeId, date, json(result));
        return result;
    }

    private Progress progress(UUID playerId, String modeId, LocalDate date) {
        List<Progress> values = jdbc.query("select attempts, clears, best_score from pve_mode_progress where player_id=? and mode_id=? and game_date=?",
                (rs, row) -> new Progress(rs.getInt("attempts"), rs.getInt("clears"), rs.getLong("best_score")), playerId, modeId, date);
        return values.isEmpty() ? new Progress(0, 0, 0L) : values.get(0);
    }

    private ResourcePveBattleView loadRun(UUID requestId) {
        List<String> json = jdbc.query("select result_json from resource_pve_runs where request_id=?", (rs, row) -> rs.getString(1), requestId);
        if (json.isEmpty()) return null;
        try { return objectMapper.readValue(json.get(0), ResourcePveBattleView.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("cannot decode resource PvE run", error); }
    }
    private String json(ResourcePveBattleView value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException error) { throw new IllegalStateException("cannot encode resource PvE run", error); }
    }
    private LocalDate gameDate() { return clock.instant().atZone(ZoneOffset.UTC).toLocalDate(); }

    private record Progress(int attempts, int clears, long bestScore) { }
    public record ResourcePveBoard(String catalogVersion, String rulesetVersion, String gameDate, int playerLevel,
                                   int energy, int energyCap, List<ResourcePveModeView> modes) { }
    public record ResourcePveModeView(String modeId, String modeType, String nameEn, String nameVi, int teamSize,
                                      int energyCost, int dailyAttemptLimit, int attemptsRemaining, int minPlayerLevel,
                                      int clearsToday, long bestScore, boolean playable, String blockedReason,
                                      long rewardGold, String rewardItemId, long rewardItemQuantity, String resetPolicy) { }
    public record ResourcePveBattleView(String requestId, String playerId, String modeId, String catalogVersion,
                                        String rulesetVersion, String gameDate, boolean replayed, int energyCost,
                                        int energyAfter, boolean won, long goldReward, String itemId, long itemQuantity,
                                        long seed, List<BattleParticipant> participants, BattleResult battle) {
        public ResourcePveBattleView withReplayed(boolean value) {
            return new ResourcePveBattleView(requestId, playerId, modeId, catalogVersion, rulesetVersion, gameDate, value,
                    energyCost, energyAfter, won, goldReward, itemId, itemQuantity, seed, participants, battle);
        }
    }
}
