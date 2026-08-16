package com.ninjaassemble.pvp.application;

import com.ninjaassemble.battle.domain.BattleRules;
import com.ninjaassemble.battle.domain.ShadowArenaSeries;
import com.ninjaassemble.battle.sim.BattleOutcome;
import com.ninjaassemble.battle.sim.BattleRequest;
import com.ninjaassemble.battle.sim.BattleResult;
import com.ninjaassemble.battle.sim.BattleRuleset;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.DeterministicBattleEngine;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.play.domain.BattleParticipant;
import com.ninjaassemble.play.domain.ExperimentalAbilityProfile;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
import com.ninjaassemble.play.domain.PassiveEffectResolver;
import com.ninjaassemble.play.domain.TechniqueEffectResolver;
import com.ninjaassemble.pvp.domain.ArenaFormationSnapshot;
import com.ninjaassemble.pvp.domain.ArenaRatingCalculator;
import com.ninjaassemble.pvp.domain.ArenaRatingProfile;
import com.ninjaassemble.pvp.domain.FormationMemberSnapshot;
import com.ninjaassemble.pvp.domain.ShadowArenaMatchResolver;
import com.ninjaassemble.pvp.domain.ShadowArenaRosterSnapshot;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class ShadowArenaApplicationService {
    public static final String SEASON_ID = "shadow-s1-experimental";
    public static final String SERIES_RULES_VERSION = "bo3-hp-power-tiebreak-v1";
    public static final String REWARD_PROFILE_VERSION = "shadow-reward-design-v1";
    private static final long INITIAL_RATING = 1_000;
    private static final long WIN_COINS = 50;
    private static final long LOSS_COINS = 20;

    private final PlayerService players;
    private final HeroOwnershipService ownership;
    private final ExperimentalCombatStatsResolver stats;
    private final WalletService wallet;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DeterministicBattleEngine engine = new DeterministicBattleEngine();
    private final ShadowArenaMatchResolver seriesResolver = new ShadowArenaMatchResolver();

    public ShadowArenaApplicationService(PlayerService players, HeroOwnershipService ownership,
                                         ExperimentalCombatStatsResolver stats, WalletService wallet,
                                         JdbcTemplate jdbc, Clock clock) {
        this.players = players;
        this.ownership = ownership;
        this.stats = stats;
        this.wallet = wallet;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public ShadowArenaState state(UUID playerId) {
        players.require(playerId);
        List<OwnedHeroView> owned = ownership.list(playerId);
        int ownedCount = owned.size();
        if (ownedCount < BattleRules.SHADOW_ROSTER_SIZE) {
            return new ShadowArenaState(SEASON_ID, false, ownedCount, BattleRules.SHADOW_ROSTER_SIZE,
                    BattleRules.SHADOW_ROSTER_SIZE - ownedCount, 0,
                    ArenaRatingProfile.experimentalV1().version(), SERIES_RULES_VERSION, REWARD_PROFILE_VERSION, List.of());
        }
        List<OwnedHeroView> roster = roster(owned);
        validateRoster(roster);
        long rating = ensureProfile(playerId, roster);
        List<ShadowOpponentView> opponents = new ArrayList<>();
        List<OpponentRow> candidates = jdbc.query("""
                select p.id, p.display_name, coalesce(sap.rating, ?) as rating,
                       (select count(*) from player_heroes ph where ph.player_id = p.id) as owned_count
                from players p
                left join shadow_arena_profiles sap on sap.player_id = p.id and sap.season_id = ?
                where p.id <> ?
                  and (select count(*) from player_heroes ph where ph.player_id = p.id) >= ?
                order by abs(coalesce(sap.rating, ?) - ?), p.created_at, p.id
                limit 12
                """, (rs, row) -> new OpponentRow(rs.getObject("id", UUID.class), rs.getString("display_name"), rs.getLong("rating"), rs.getInt("owned_count")),
                INITIAL_RATING, SEASON_ID, playerId, BattleRules.SHADOW_ROSTER_SIZE, INITIAL_RATING, rating);
        for (OpponentRow candidate : candidates) {
            List<OwnedHeroView> candidateRoster = ownership.list(candidate.playerId());
            if (candidateRoster.size() < BattleRules.SHADOW_ROSTER_SIZE) continue;
            List<OwnedHeroView> top = roster(candidateRoster);
            validateRoster(top);
            ensureProfile(candidate.playerId(), top);
            opponents.add(new ShadowOpponentView(candidate.playerId(), candidate.displayName(), candidate.rating(), totalPower(top), false));
            if (opponents.size() == 5) break;
        }
        if (opponents.isEmpty()) opponents.add(new ShadowOpponentView(playerId, "Training Mirror", rating, totalPower(roster), true));
        return new ShadowArenaState(SEASON_ID, true, ownedCount, BattleRules.SHADOW_ROSTER_SIZE, 0, rating,
                ArenaRatingProfile.experimentalV1().version(), SERIES_RULES_VERSION, REWARD_PROFILE_VERSION, List.copyOf(opponents));
    }

    @Transactional
    public ShadowArenaBattleView fight(UUID playerId, UUID opponentPlayerId) {
        if (opponentPlayerId == null) throw new IllegalArgumentException("opponentPlayerId is required");
        players.require(playerId);
        players.require(opponentPlayerId);
        List<OwnedHeroView> playerOwned = ownership.list(playerId);
        if (playerOwned.size() < BattleRules.SHADOW_ROSTER_SIZE) throw new IllegalStateException("Shadow Arena requires 15 owned ninja");
        List<OwnedHeroView> opponentOwned = ownership.list(opponentPlayerId);
        if (opponentOwned.size() < BattleRules.SHADOW_ROSTER_SIZE) throw new IllegalStateException("Shadow Arena opponent requires 15 owned ninja");

        List<OwnedHeroView> playerRoster = roster(playerOwned);
        List<OwnedHeroView> opponentRoster = roster(opponentOwned);
        validateRoster(playerRoster);
        validateRoster(opponentRoster);
        boolean training = playerId.equals(opponentPlayerId);
        long ratingBefore = ensureProfile(playerId, playerRoster);
        long opponentRating = training ? ratingBefore : ensureProfile(opponentPlayerId, opponentRoster);
        long masterSeed = secureRandom.nextLong();
        SplittableRandom waveSeeds = new SplittableRandom(masterSeed);
        BattleRuleset ruleset = BattleRuleset.experimentalV1();
        List<Boolean> wins = new ArrayList<>();
        List<ShadowSquadBattleView> squadBattles = new ArrayList<>();
        ShadowArenaSeries.SeriesWinner winner = ShadowArenaSeries.SeriesWinner.UNDECIDED;
        for (int squadIndex = 0; squadIndex < BattleRules.SHADOW_SQUAD_COUNT && winner == ShadowArenaSeries.SeriesWinner.UNDECIDED; squadIndex++) {
            List<OwnedHeroView> playerSquad = playerRoster.subList(squadIndex * 5, squadIndex * 5 + 5);
            List<OwnedHeroView> opponentSquad = opponentRoster.subList(squadIndex * 5, squadIndex * 5 + 5);
            long squadSeed = waveSeeds.nextLong();
            SquadBuild build = buildSquadBattle(playerSquad, opponentSquad, squadIndex);
            BattleResult battle = engine.simulate(new BattleRequest(squadSeed, ruleset, build.units()));
            SquadDecision decision = squadDecision(battle, build.participants(), playerSquad, opponentSquad);
            wins.add(decision.playerWon());
            squadBattles.add(new ShadowSquadBattleView(squadIndex + 1, squadSeed, decision.playerWon(), decision.tiebreak(), build.participants(), battle));
            winner = seriesResolver.resolve(wins);
        }
        if (winner == ShadowArenaSeries.SeriesWinner.UNDECIDED) throw new IllegalStateException("Shadow Arena series failed to resolve after three squads");

        boolean playerWonSeries = winner == ShadowArenaSeries.SeriesWinner.PLAYER;
        ArenaRatingProfile ratingProfile = ArenaRatingProfile.experimentalV1();
        ArenaRatingCalculator.RatingResult ratingResult = training
                ? new ArenaRatingCalculator.RatingResult(ratingBefore, ratingBefore, 0, ratingProfile.version())
                : ArenaRatingCalculator.resolve(ratingBefore, playerWonSeries, ratingProfile);
        if (!training) jdbc.update("update shadow_arena_profiles set rating = ?, roster_snapshot = cast(? as jsonb), updated_at = ? where player_id = ? and season_id = ?",
                ratingResult.after(), rosterJson(playerRoster), clock.instant(), playerId, SEASON_ID);

        UUID battleId = UUID.randomUUID();
        long reward = training ? 0 : playerWonSeries ? WIN_COINS : LOSS_COINS;
        String rewardKey = reward > 0 ? "shadow:" + battleId + ":coin" : null;
        if (reward > 0) wallet.mutate(playerId, Currency.SHADOW_COIN, reward, "SHADOW_ARENA_REWARD", SEASON_ID, rewardKey);
        jdbc.update("""
                insert into shadow_arena_battles(id, challenger_id, opponent_id, season_id, squad_results, winner, reward_grant_key, created_at)
                values (?, ?, ?, ?, cast(? as jsonb), ?, ?, ?)
                """, battleId, playerId, opponentPlayerId, SEASON_ID, squadResultsJson(masterSeed, ratingResult, squadBattles), winner.name(), rewardKey, clock.instant());
        return new ShadowArenaBattleView(battleId, SEASON_ID, training, opponentPlayerId, opponentRating, totalPower(opponentRoster),
                masterSeed, winner.name(), ratingResult.before(), ratingResult.after(), ratingResult.delta(), ratingProfile.version(),
                SERIES_RULES_VERSION, reward, REWARD_PROFILE_VERSION, ExperimentalCombatStatsResolver.VERSION,
                ExperimentalAbilityProfile.VERSION, TechniqueEffectResolver.VERSION, PassiveEffectResolver.VERSION, List.copyOf(squadBattles));
    }

    private long ensureProfile(UUID playerId, List<OwnedHeroView> roster) {
        jdbc.update("""
                insert into shadow_arena_profiles(player_id, season_id, rating, roster_snapshot, updated_at)
                values (?, ?, ?, cast(? as jsonb), ?)
                on conflict (player_id, season_id)
                do update set roster_snapshot = excluded.roster_snapshot, updated_at = excluded.updated_at
                """, playerId, SEASON_ID, INITIAL_RATING, rosterJson(roster), clock.instant());
        Long rating = jdbc.queryForObject("select rating from shadow_arena_profiles where player_id = ? and season_id = ?", Long.class, playerId, SEASON_ID);
        return rating == null ? INITIAL_RATING : rating;
    }

    private SquadBuild buildSquadBattle(List<OwnedHeroView> player, List<OwnedHeroView> opponent, int squadIndex) {
        List<BattleUnitSeed> units = new ArrayList<>();
        List<BattleParticipant> participants = new ArrayList<>();
        addSquad(player, TeamSide.A, "shadow:A:" + squadIndex + ":", units, participants);
        addSquad(opponent, TeamSide.B, "shadow:B:" + squadIndex + ":", units, participants);
        return new SquadBuild(List.copyOf(units), List.copyOf(participants));
    }

    private void addSquad(List<OwnedHeroView> squad, TeamSide side, String prefix, List<BattleUnitSeed> units, List<BattleParticipant> participants) {
        for (int slot = 0; slot < squad.size(); slot++) {
            OwnedHeroView hero = squad.get(slot);
            BattleUnitSeed unit = stats.resolve(prefix + hero.id(), hero.characterId(), hero.currentVariant(), hero.level(), side, slot);
            units.add(unit);
            participants.add(new BattleParticipant(unit.id(), hero.characterId(), hero.displayName(), hero.currentVariant(), hero.level(), unit.side(), unit.slot(), unit.maxHp()));
        }
    }

    private static SquadDecision squadDecision(BattleResult battle, List<BattleParticipant> participants, List<OwnedHeroView> playerSquad, List<OwnedHeroView> opponentSquad) {
        if (battle.outcome() == BattleOutcome.TEAM_A) return new SquadDecision(true, "NONE");
        if (battle.outcome() == BattleOutcome.TEAM_B) return new SquadDecision(false, "NONE");
        long playerHp = remainingHp(battle, participants, TeamSide.A);
        long opponentHp = remainingHp(battle, participants, TeamSide.B);
        if (playerHp != opponentHp) return new SquadDecision(playerHp > opponentHp, "TOTAL_HP");
        long playerPower = totalPower(playerSquad);
        long opponentPower = totalPower(opponentSquad);
        if (playerPower != opponentPower) return new SquadDecision(playerPower > opponentPower, "SQUAD_POWER");
        return new SquadDecision(true, "PLAYER_SEED_ORDER");
    }

    private static long remainingHp(BattleResult battle, List<BattleParticipant> participants, TeamSide side) {
        return participants.stream().filter(it -> it.side() == side).mapToLong(it -> battle.finalHp().getOrDefault(it.battleUnitId(), 0L)).sum();
    }
    private static List<OwnedHeroView> roster(List<OwnedHeroView> owned) { return List.copyOf(owned.subList(0, BattleRules.SHADOW_ROSTER_SIZE)); }
    private static void validateRoster(List<OwnedHeroView> roster) {
        List<ArenaFormationSnapshot> squads = new ArrayList<>();
        for (int squad = 0; squad < BattleRules.SHADOW_SQUAD_COUNT; squad++) {
            List<FormationMemberSnapshot> members = new ArrayList<>();
            for (OwnedHeroView hero : roster.subList(squad * 5, squad * 5 + 5)) members.add(new FormationMemberSnapshot(hero.id().toString(), hero.characterId(), hero.currentVariant(), heroPower(hero)));
            squads.add(new ArenaFormationSnapshot(members));
        }
        new ShadowArenaRosterSnapshot(squads);
    }
    private static long heroPower(OwnedHeroView hero) { return hero.level() * 1_000L + hero.awakeningLevel() * 250L + 500L; }
    private static long totalPower(List<OwnedHeroView> roster) { return roster.stream().mapToLong(ShadowArenaApplicationService::heroPower).sum(); }
    private static String rosterJson(List<OwnedHeroView> roster) {
        String squads = java.util.stream.IntStream.range(0, BattleRules.SHADOW_SQUAD_COUNT).mapToObj(squad -> roster.subList(squad * 5, squad * 5 + 5).stream()
                .map(hero -> "{\"playerHeroId\":\"" + hero.id() + "\",\"characterId\":\"" + escape(hero.characterId()) + "\",\"variant\":\"" + escape(hero.currentVariant() == null ? "BASE" : hero.currentVariant()) + "\",\"level\":" + hero.level() + ",\"power\":" + heroPower(hero) + "}")
                .collect(Collectors.joining(",", "[", "]"))).collect(Collectors.joining(",", "[", "]"));
        return "{\"version\":\"shadow-roster-auto-v1\",\"squads\":" + squads + "}";
    }
    private static String squadResultsJson(long masterSeed, ArenaRatingCalculator.RatingResult rating, List<ShadowSquadBattleView> battles) {
        String rows = battles.stream().map(value -> "{\"squad\":" + value.squadIndex() + ",\"seed\":" + value.seed() + ",\"playerWon\":" + value.playerWon() + ",\"tiebreak\":\"" + value.tiebreak() + "\",\"outcome\":\"" + value.battle().outcome().name() + "\",\"rounds\":" + value.battle().rounds() + "}").collect(Collectors.joining(",", "[", "]"));
        return "{\"masterSeed\":" + masterSeed + ",\"seriesRulesVersion\":\"" + SERIES_RULES_VERSION + "\",\"ratingBefore\":" + rating.before() + ",\"ratingAfter\":" + rating.after() + ",\"squads\":" + rows + "}";
    }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private record OpponentRow(UUID playerId, String displayName, long rating, int ownedCount) {}
    private record SquadBuild(List<BattleUnitSeed> units, List<BattleParticipant> participants) {}
    private record SquadDecision(boolean playerWon, String tiebreak) {}
    public record ShadowArenaState(String seasonId, boolean eligible, int ownedCount, int requiredCount, int missingCount, long rating, String ratingProfileVersion, String seriesRulesVersion, String rewardProfileVersion, List<ShadowOpponentView> opponents) {}
    public record ShadowOpponentView(UUID playerId, String displayName, long rating, long totalPower, boolean training) {}
    public record ShadowSquadBattleView(int squadIndex, long seed, boolean playerWon, String tiebreak, List<BattleParticipant> participants, BattleResult battle) {}
    public record ShadowArenaBattleView(UUID battleId, String seasonId, boolean training, UUID opponentPlayerId, long opponentRating, long opponentPower, long masterSeed, String winner, long ratingBefore, long ratingAfter, long ratingDelta, String ratingProfileVersion, String seriesRulesVersion, long shadowCoinReward, String rewardProfileVersion, String combatStatsVersion, String abilityProfileVersion, String techniqueMappingVersion, String passiveProfileVersion, List<ShadowSquadBattleView> squads) {}
}
