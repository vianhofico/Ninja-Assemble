package com.ninjaassemble.pvp.application;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical M63 Arena path: defense snapshots, monthly season, history and idempotent battle requests. */
@Service
public final class ProductionArenaService {
    public static final String RULESET_VERSION = "arena-production-realtime-v1";
    public static final String REWARD_PROFILE_VERSION = "arena-reward-v1";
    private static final long WIN_COINS = 30L;
    private static final long LOSS_COINS = 10L;

    private final PlayerService players;
    private final FormationService offenseFormations;
    private final CompetitiveFormationService defenses;
    private final CompetitiveSeasonService seasons;
    private final ExperimentalCombatStatsResolver stats;
    private final WalletService wallet;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RealtimeBattleEngine engine = new RealtimeBattleEngine();

    public ProductionArenaService(PlayerService players, FormationService offenseFormations,
                                  CompetitiveFormationService defenses, CompetitiveSeasonService seasons,
                                  ExperimentalCombatStatsResolver stats, WalletService wallet,
                                  JdbcTemplate jdbc, Clock clock, ObjectMapper objectMapper) {
        this.players = players; this.offenseFormations = offenseFormations; this.defenses = defenses;
        this.seasons = seasons; this.stats = stats; this.wallet = wallet; this.jdbc = jdbc;
        this.clock = clock; this.objectMapper = objectMapper;
    }

    @Transactional
    public ArenaState state(UUID playerId) {
        PlayerEntity player = players.require(playerId);
        long rating = seasons.ensureArenaProfile(playerId);
        CompetitiveFormationService.ArenaDefense ownDefense = defenses.ensureArenaDefense(playerId);
        String seasonId = seasons.currentSeasonId(CompetitiveSeasonService.Mode.ARENA);
        List<ArenaOpponentView> opponents = new ArrayList<>();
        List<OpponentRow> rows = jdbc.query("""
                select p.id, p.display_name, ap.rating
                from arena_profiles ap join players p on p.id=ap.player_id
                where ap.season_id=? and ap.player_id<>? and ap.defense_formation_id is not null
                order by abs(ap.rating-?), ap.updated_at, ap.player_id
                limit 12
                """, (rs, row) -> new OpponentRow(rs.getObject("id", UUID.class), rs.getString("display_name"), rs.getLong("rating")),
                seasonId, playerId, rating);
        for (OpponentRow row : rows) {
            CompetitiveFormationService.ArenaDefense defense = defenses.loadArenaDefense(row.playerId());
            if (defense.heroes().size() != 5) continue;
            opponents.add(new ArenaOpponentView(row.playerId(), row.displayName(), row.rating(), power(defense.heroes()), false));
            if (opponents.size() == 5) break;
        }
        if (opponents.isEmpty() && ownDefense.heroes().size() == 5)
            opponents.add(new ArenaOpponentView(playerId, player.getDisplayName() + " Mirror", rating, power(ownDefense.heroes()), true));
        CompetitiveSeasonService.SeasonRewardState reward = seasons.previousReward(playerId, CompetitiveSeasonService.Mode.ARENA);
        return new ArenaState(seasonId, seasons.currentSeasonEndsAt(), rating, ArenaRatingProfile.experimentalV1().version(),
                REWARD_PROFILE_VERSION, ownDefense.heroes().size() == 5, reward, List.copyOf(opponents));
    }

    @Transactional
    public DefenseView saveDefense(UUID playerId, List<UUID> heroIds) {
        players.require(playerId); seasons.ensureArenaProfile(playerId);
        CompetitiveFormationService.ArenaDefense saved = defenses.saveArenaDefense(playerId, heroIds);
        return new DefenseView(saved.formationId(), saved.heroes());
    }

    @Transactional
    public ArenaBattleView fight(UUID playerId, UUID opponentPlayerId, UUID requestId) {
        if (opponentPlayerId == null) throw new IllegalArgumentException("opponentPlayerId is required");
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        players.require(playerId); PlayerEntity opponent = players.require(opponentPlayerId);
        long lockKey = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits() ^ 0x4152454e41L;
        jdbc.query("select pg_advisory_xact_lock(?)", rs -> { }, lockKey);
        ArenaBattleView replay = loadRequest(requestId);
        if (replay != null) {
            if (!replay.playerId().equals(playerId) || !replay.opponentPlayerId().equals(opponentPlayerId))
                throw new IllegalStateException("Arena requestId already belongs to another battle");
            return replay.withReplayed(true);
        }

        FormationService.FormationView offense = offenseFormations.load(playerId);
        if (offense.heroes().size() != 5) throw new IllegalStateException("Arena offense requires five ninja");
        CompetitiveFormationService.ArenaDefense opponentDefense = defenses.ensureArenaDefense(opponentPlayerId);
        if (opponentDefense.heroes().size() != 5) throw new IllegalStateException("Arena opponent has no five-ninja defense formation");

        String seasonId = seasons.currentSeasonId(CompetitiveSeasonService.Mode.ARENA);
        boolean training = playerId.equals(opponentPlayerId);
        long ratingBefore = seasons.ensureArenaProfile(playerId);
        long opponentRating = training ? ratingBefore : seasons.ensureArenaProfile(opponentPlayerId);
        long opponentPower = power(opponentDefense.heroes());

        UUID snapshotId = UUID.randomUUID();
        jdbc.update("""
                insert into arena_opponent_snapshots(id, player_id, opponent_player_id, season_id, opponent_rating, formation_snapshot, captured_at)
                values (?, ?, ?, ?, ?, jsonb_build_object('roster', ?, 'power', ?), ?)
                """, snapshotId, playerId, opponentPlayerId, seasonId, opponentRating,
                rosterString(opponentDefense.heroes()), opponentPower, clock.instant());

        List<BattleUnitSeed> units = new ArrayList<>(); List<BattleParticipant> participants = new ArrayList<>();
        addHeroes(offense.heroes(), TeamSide.A, "arena:A:", units, participants);
        addHeroes(opponentDefense.heroes(), TeamSide.B, "arena:B:", units, participants);
        long seed = secureRandom.nextLong(); BattleRuleset ruleset = BattleRuleset.experimentalV1();
        BattleResult battle = engine.simulate(new RealtimeBattleRequest(seed, ruleset, units));

        ArenaRatingProfile ratingProfile = ArenaRatingProfile.experimentalV1();
        ArenaRatingCalculator.RatingResult rating = training || battle.outcome() == BattleOutcome.DRAW
                ? new ArenaRatingCalculator.RatingResult(ratingBefore, ratingBefore, 0, ratingProfile.version())
                : ArenaRatingCalculator.resolve(ratingBefore, battle.outcome() == BattleOutcome.TEAM_A, ratingProfile);
        if (!training && battle.outcome() != BattleOutcome.DRAW)
            jdbc.update("update arena_profiles set rating=?, updated_at=? where player_id=? and season_id=?",
                    rating.after(), clock.instant(), playerId, seasonId);

        UUID battleId = UUID.randomUUID();
        long coins = training ? 0L : battle.outcome() == BattleOutcome.TEAM_A ? WIN_COINS : LOSS_COINS;
        String rewardKey = coins > 0 ? "arena:" + requestId + ":coin" : null;
        if (coins > 0) wallet.mutate(playerId, Currency.ARENA_COIN, coins, "ARENA_BATTLE_REWARD", seasonId, rewardKey);
        jdbc.update("""
                insert into arena_battles(id, challenger_id, opponent_id, opponent_snapshot_id, battle_seed, ruleset_version,
                                          rating_profile_version, result, rating_before, rating_after, reward_grant_key, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, battleId, playerId, opponentPlayerId, snapshotId, seed, RULESET_VERSION,
                ratingProfile.version(), battle.outcome().name(), rating.before(), rating.after(), rewardKey, clock.instant());

        ArenaBattleView result = new ArenaBattleView(requestId, battleId, playerId, seasonId, false, training,
                opponentPlayerId, opponent.getDisplayName(), opponentRating, opponentPower, rating.before(), rating.after(),
                rating.delta(), ratingProfile.version(), coins, REWARD_PROFILE_VERSION,
                ExperimentalCombatStatsResolver.VERSION, ExperimentalAbilityProfile.VERSION,
                TechniqueEffectResolver.VERSION, PassiveEffectResolver.VERSION, List.copyOf(participants), battle);
        persistRequest(requestId, playerId, opponentPlayerId, seasonId, result);
        return result;
    }

    @Transactional(readOnly = true)
    public List<HistoryItem> history(UUID playerId, int limit) {
        players.require(playerId); int safeLimit = Math.max(1, Math.min(50, limit));
        return jdbc.query("""
                select ab.id, ab.challenger_id, ab.opponent_id, other.display_name as other_name,
                       coalesce(aos.season_id, '') as season_id, ab.result, ab.rating_before, ab.rating_after, ab.created_at
                from arena_battles ab
                join players other on other.id = case when ab.challenger_id=? then ab.opponent_id else ab.challenger_id end
                left join arena_opponent_snapshots aos on aos.id=ab.opponent_snapshot_id
                where ab.challenger_id=? or ab.opponent_id=?
                order by ab.created_at desc limit ?
                """, (rs, row) -> new HistoryItem(
                rs.getObject("id", UUID.class), rs.getObject("challenger_id", UUID.class).equals(playerId) ? "ATTACK" : "DEFENSE",
                rs.getString("other_name"), rs.getString("season_id"), rs.getString("result"),
                rs.getLong("rating_before"), rs.getLong("rating_after"), rs.getTimestamp("created_at").toInstant()),
                playerId, playerId, playerId, safeLimit);
    }

    public CompetitiveSeasonService.SeasonRewardState claimPreviousSeason(UUID playerId) {
        players.require(playerId); seasons.ensureArenaProfile(playerId);
        return seasons.claimPrevious(playerId, CompetitiveSeasonService.Mode.ARENA);
    }

    private ArenaBattleView loadRequest(UUID requestId) {
        List<String> rows = jdbc.query("select result_json from competitive_battle_requests where request_id=? and mode='ARENA'",
                (rs, row) -> rs.getString(1), requestId);
        if (rows.isEmpty()) return null;
        try { return objectMapper.readValue(rows.get(0), ArenaBattleView.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("cannot decode Arena request result", error); }
    }
    private void persistRequest(UUID requestId, UUID playerId, UUID opponentId, String seasonId, ArenaBattleView value) {
        try {
            jdbc.update("""
                    insert into competitive_battle_requests(request_id, mode, player_id, opponent_player_id, season_id, result_json)
                    values (?, 'ARENA', ?, ?, ?, ?)
                    """, requestId, playerId, opponentId, seasonId, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException error) { throw new IllegalStateException("cannot encode Arena request result", error); }
    }
    private void addHeroes(List<OwnedHeroView> heroes, TeamSide side, String prefix, List<BattleUnitSeed> units, List<BattleParticipant> participants) {
        for (int slot=0; slot<heroes.size(); slot++) {
            OwnedHeroView hero=heroes.get(slot); BattleUnitSeed unit=stats.resolve(prefix+hero.id(), hero.heroId(), hero.awakened(), hero.level(), side, slot);
            units.add(unit); participants.add(BattleParticipant.heroVersion(unit.id(), hero.characterId(), hero.heroId(), hero.awakened(),
                    hero.awakeningId(), hero.displayName(), hero.level(), unit.side(), unit.slot(), unit.maxHp()));
        }
    }
    private static long power(List<OwnedHeroView> heroes) { return heroes.stream().mapToLong(h -> h.level()*1_000L+(h.awakened()?250L:0L)+500L).sum(); }
    private static String rosterString(List<OwnedHeroView> heroes) { return String.join("|", heroes.stream().map(h -> h.id()+":"+h.heroId()+":"+(h.awakened()?"AWAKENED":"NORMAL")+":"+h.level()).toList()); }

    private record OpponentRow(UUID playerId, String displayName, long rating) { }
    public record ArenaOpponentView(UUID playerId, String displayName, long rating, long power, boolean training) { }
    public record ArenaState(String seasonId, Instant seasonEndsAt, long rating, String ratingProfileVersion, String rewardProfileVersion,
                             boolean defenseConfigured, CompetitiveSeasonService.SeasonRewardState previousSeasonReward,
                             List<ArenaOpponentView> opponents) { }
    public record DefenseView(UUID formationId, List<OwnedHeroView> heroes) { }
    public record ArenaBattleView(UUID requestId, UUID battleId, UUID playerId, String seasonId, boolean replayed, boolean training,
                                  UUID opponentPlayerId, String opponentDisplayName, long opponentRating, long opponentPower,
                                  long ratingBefore, long ratingAfter, long ratingDelta, String ratingProfileVersion,
                                  long arenaCoinReward, String rewardProfileVersion, String combatStatsVersion,
                                  String abilityProfileVersion, String techniqueMappingVersion, String passiveProfileVersion,
                                  List<BattleParticipant> participants, BattleResult battle) {
        public ArenaBattleView withReplayed(boolean value) { return new ArenaBattleView(requestId,battleId,playerId,seasonId,value,training,
                opponentPlayerId,opponentDisplayName,opponentRating,opponentPower,ratingBefore,ratingAfter,ratingDelta,ratingProfileVersion,
                arenaCoinReward,rewardProfileVersion,combatStatsVersion,abilityProfileVersion,techniqueMappingVersion,passiveProfileVersion,participants,battle); }
    }
    public record HistoryItem(UUID battleId, String role, String opponentDisplayName, String seasonId, String result,
                              long ratingBefore, long ratingAfter, Instant createdAt) { }
}
