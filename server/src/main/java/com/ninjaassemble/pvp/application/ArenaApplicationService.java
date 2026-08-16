package com.ninjaassemble.pvp.application;

import com.ninjaassemble.battle.sim.BattleOutcome;
import com.ninjaassemble.battle.sim.BattleRequest;
import com.ninjaassemble.battle.sim.BattleResult;
import com.ninjaassemble.battle.sim.BattleRuleset;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.DeterministicBattleEngine;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.player.domain.PlayerEntity;
import com.ninjaassemble.play.application.FormationService;
import com.ninjaassemble.play.domain.BattleParticipant;
import com.ninjaassemble.play.domain.ExperimentalAbilityProfile;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
import com.ninjaassemble.play.domain.PassiveEffectResolver;
import com.ninjaassemble.play.domain.TechniqueEffectResolver;
import com.ninjaassemble.pvp.domain.ArenaRatingCalculator;
import com.ninjaassemble.pvp.domain.ArenaRatingProfile;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class ArenaApplicationService {
    public static final String SEASON_ID = "arena-s1-experimental";
    public static final String REWARD_PROFILE_VERSION = "arena-reward-design-v1";
    private static final long INITIAL_RATING = 1_000;
    private static final long WIN_COINS = 30;
    private static final long LOSS_COINS = 10;

    private final PlayerService players;
    private final FormationService formations;
    private final ExperimentalCombatStatsResolver stats;
    private final WalletService wallet;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DeterministicBattleEngine engine = new DeterministicBattleEngine();

    public ArenaApplicationService(PlayerService players, FormationService formations, ExperimentalCombatStatsResolver stats,
                                   WalletService wallet, JdbcTemplate jdbc, Clock clock) {
        this.players = players;
        this.formations = formations;
        this.stats = stats;
        this.wallet = wallet;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public ArenaState state(UUID playerId) {
        PlayerEntity player = players.require(playerId);
        long rating = ensureProfile(playerId);
        List<ArenaOpponentView> opponents = new ArrayList<>();
        List<OpponentRow> candidates = jdbc.query("""
                select p.id, p.display_name, coalesce(ap.rating, ?) as rating
                from players p
                left join arena_profiles ap on ap.player_id = p.id and ap.season_id = ?
                where p.id <> ?
                order by abs(coalesce(ap.rating, ?) - ?), p.created_at, p.id
                limit 12
                """, (rs, row) -> new OpponentRow(
                rs.getObject("id", UUID.class), rs.getString("display_name"), rs.getLong("rating")),
                INITIAL_RATING, SEASON_ID, playerId, INITIAL_RATING, rating);
        for (OpponentRow candidate : candidates) {
            FormationService.FormationView formation = formations.load(candidate.playerId());
            if (formation.heroes().size() != 5) continue;
            opponents.add(new ArenaOpponentView(candidate.playerId(), candidate.displayName(), candidate.rating(), power(formation), false));
            if (opponents.size() == 5) break;
        }
        if (opponents.isEmpty()) {
            FormationService.FormationView formation = formations.load(playerId);
            if (formation.heroes().size() == 5)
                opponents.add(new ArenaOpponentView(playerId, player.getDisplayName() + " Mirror", rating, power(formation), true));
        }
        ArenaRatingProfile ratingProfile = ArenaRatingProfile.experimentalV1();
        return new ArenaState(SEASON_ID, rating, ratingProfile.version(), REWARD_PROFILE_VERSION, List.copyOf(opponents));
    }

    @Transactional
    public ArenaBattleView fight(UUID playerId, UUID opponentPlayerId) {
        if (opponentPlayerId == null) throw new IllegalArgumentException("opponentPlayerId is required");
        PlayerEntity challenger = players.require(playerId);
        PlayerEntity opponent = players.require(opponentPlayerId);
        FormationService.FormationView challengerFormation = formations.load(playerId);
        FormationService.FormationView opponentFormation = formations.load(opponentPlayerId);
        if (challengerFormation.heroes().size() != 5) throw new IllegalStateException("save a five-ninja formation before Arena");
        if (opponentFormation.heroes().size() != 5) throw new IllegalStateException("Arena opponent has no five-ninja defense snapshot");

        boolean training = playerId.equals(opponentPlayerId);
        long ratingBefore = ensureProfile(playerId);
        long opponentRating = training ? ratingBefore : ensureProfile(opponentPlayerId);
        long opponentPower = power(opponentFormation);
        UUID snapshotId = UUID.randomUUID();
        jdbc.update("""
                insert into arena_opponent_snapshots(id, player_id, opponent_player_id, season_id, opponent_rating, formation_snapshot, captured_at)
                values (?, ?, ?, ?, ?, jsonb_build_object('roster', ?, 'power', ?), ?)
                """, snapshotId, playerId, opponentPlayerId, SEASON_ID, opponentRating, rosterString(opponentFormation), opponentPower, clock.instant());

        List<BattleUnitSeed> units = new ArrayList<>();
        List<BattleParticipant> participants = new ArrayList<>();
        addFormation(challengerFormation, TeamSide.A, "arena:A:", units, participants);
        addFormation(opponentFormation, TeamSide.B, "arena:B:", units, participants);
        long seed = secureRandom.nextLong();
        BattleRuleset ruleset = BattleRuleset.experimentalV1();
        BattleResult battle = engine.simulate(new BattleRequest(seed, ruleset, units));

        ArenaRatingProfile ratingProfile = ArenaRatingProfile.experimentalV1();
        ArenaRatingCalculator.RatingResult ratingResult;
        if (training || battle.outcome() == BattleOutcome.DRAW) {
            ratingResult = new ArenaRatingCalculator.RatingResult(ratingBefore, ratingBefore, 0, ratingProfile.version());
        } else {
            ratingResult = ArenaRatingCalculator.resolve(ratingBefore, battle.outcome() == BattleOutcome.TEAM_A, ratingProfile);
            jdbc.update("update arena_profiles set rating = ?, updated_at = ? where player_id = ?",
                    ratingResult.after(), clock.instant(), playerId);
        }

        UUID battleId = UUID.randomUUID();
        long arenaCoins = training ? 0 : battle.outcome() == BattleOutcome.TEAM_A ? WIN_COINS : LOSS_COINS;
        String rewardKey = arenaCoins > 0 ? "arena:" + battleId + ":coin" : null;
        if (arenaCoins > 0)
            wallet.mutate(playerId, Currency.ARENA_COIN, arenaCoins, "ARENA_BATTLE_REWARD", SEASON_ID, rewardKey);

        jdbc.update("""
                insert into arena_battles(id, challenger_id, opponent_id, opponent_snapshot_id, battle_seed, ruleset_version,
                                          rating_profile_version, result, rating_before, rating_after, reward_grant_key, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, battleId, playerId, opponentPlayerId, snapshotId, seed, ruleset.version(), ratingProfile.version(),
                battle.outcome().name(), ratingResult.before(), ratingResult.after(), rewardKey, clock.instant());

        return new ArenaBattleView(
                battleId, SEASON_ID, training, opponentPlayerId, opponent.getDisplayName(), opponentRating, opponentPower,
                ratingResult.before(), ratingResult.after(), ratingResult.delta(), ratingProfile.version(),
                arenaCoins, REWARD_PROFILE_VERSION,
                ExperimentalCombatStatsResolver.VERSION, ExperimentalAbilityProfile.VERSION,
                TechniqueEffectResolver.VERSION, PassiveEffectResolver.VERSION,
                List.copyOf(participants), battle);
    }

    private long ensureProfile(UUID playerId) {
        jdbc.update("""
                insert into arena_profiles(player_id, season_id, rating, updated_at)
                values (?, ?, ?, ?)
                on conflict (player_id) do nothing
                """, playerId, SEASON_ID, INITIAL_RATING, clock.instant());
        Long rating = jdbc.queryForObject("select rating from arena_profiles where player_id = ?", Long.class, playerId);
        return rating == null ? INITIAL_RATING : rating;
    }

    private void addFormation(FormationService.FormationView formation, TeamSide side, String prefix,
                              List<BattleUnitSeed> units, List<BattleParticipant> participants) {
        for (int slot = 0; slot < formation.heroes().size(); slot++) {
            OwnedHeroView hero = formation.heroes().get(slot);
            BattleUnitSeed unit = stats.resolve(prefix + hero.id(), hero.heroId(), hero.awakened(), hero.level(), side, slot);
            units.add(unit);
            participants.add(BattleParticipant.heroVersion(
                    unit.id(), hero.characterId(), hero.heroId(), hero.awakened(), hero.awakeningId(),
                    hero.displayName(), hero.level(), unit.side(), unit.slot(), unit.maxHp()));
        }
    }

    private static long power(FormationService.FormationView formation) {
        return formation.heroes().stream().mapToLong(hero -> hero.level() * 1_000L + (hero.awakened() ? 250L : 0L) + 500L).sum();
    }

    private static String rosterString(FormationService.FormationView formation) {
        return String.join("|", formation.heroes().stream().map(hero ->
                hero.id() + ":" + hero.heroId() + ":" + (hero.awakened() ? "AWAKENED" : "NORMAL") + ":" + hero.level()).toList());
    }

    private record OpponentRow(UUID playerId, String displayName, long rating) {}

    public record ArenaState(String seasonId, long rating, String ratingProfileVersion, String rewardProfileVersion,
                             List<ArenaOpponentView> opponents) {}
    public record ArenaOpponentView(UUID playerId, String displayName, long rating, long power, boolean training) {}
    public record ArenaBattleView(
            UUID battleId, String seasonId, boolean training, UUID opponentPlayerId, String opponentDisplayName,
            long opponentRating, long opponentPower, long ratingBefore, long ratingAfter, long ratingDelta,
            String ratingProfileVersion, long arenaCoinReward, String rewardProfileVersion,
            String combatStatsVersion, String abilityProfileVersion, String techniqueMappingVersion, String passiveProfileVersion,
            List<BattleParticipant> participants, BattleResult battle) {}
}
