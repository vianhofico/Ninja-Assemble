package com.ninjaassemble.pvp.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ninjaassemble.battle.domain.BattleRules;
import com.ninjaassemble.battle.sim.BattleOutcome;
import com.ninjaassemble.battle.sim.BattleResult;
import com.ninjaassemble.battle.sim.BattleRuleset;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.RealtimeBattleEngine;
import com.ninjaassemble.battle.sim.RealtimeBattleRequest;
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
import com.ninjaassemble.pvp.domain.ArenaRatingCalculator;
import com.ninjaassemble.pvp.domain.ArenaRatingProfile;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Canonical M63 Shadow Arena path with explicit 15-ninja defense, monthly seasons and idempotent series requests. */
@Service
public final class ProductionShadowArenaService {
    public static final String SERIES_RULES_VERSION = "shadow-bo3-realtime-v3";
    public static final String REWARD_PROFILE_VERSION = "shadow-reward-v1";
    private static final long WIN_COINS = 50L;
    private static final long LOSS_COINS = 20L;

    private final PlayerService players;
    private final HeroOwnershipService ownership;
    private final CompetitiveFormationService defenses;
    private final CompetitiveSeasonService seasons;
    private final ExperimentalCombatStatsResolver stats;
    private final WalletService wallet;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final RealtimeBattleEngine engine = new RealtimeBattleEngine();

    public ProductionShadowArenaService(PlayerService players, HeroOwnershipService ownership,
                                        CompetitiveFormationService defenses, CompetitiveSeasonService seasons,
                                        ExperimentalCombatStatsResolver stats, WalletService wallet,
                                        JdbcTemplate jdbc, Clock clock, ObjectMapper objectMapper) {
        this.players=players; this.ownership=ownership; this.defenses=defenses; this.seasons=seasons; this.stats=stats;
        this.wallet=wallet; this.jdbc=jdbc; this.clock=clock; this.objectMapper=objectMapper;
    }

    @Transactional
    public ShadowArenaState state(UUID playerId) {
        players.require(playerId); List<OwnedHeroView> owned=ownership.list(playerId); int ownedCount=owned.size();
        String seasonId=seasons.currentSeasonId(CompetitiveSeasonService.Mode.SHADOW_ARENA);
        if (ownedCount<BattleRules.SHADOW_ROSTER_SIZE) {
            return new ShadowArenaState(seasonId,seasons.currentSeasonEndsAt(),false,ownedCount,BattleRules.SHADOW_ROSTER_SIZE,
                    BattleRules.SHADOW_ROSTER_SIZE-ownedCount,0L,ArenaRatingProfile.experimentalV1().version(),SERIES_RULES_VERSION,
                    REWARD_PROFILE_VERSION,false,seasons.previousReward(playerId,CompetitiveSeasonService.Mode.SHADOW_ARENA),List.of());
        }
        CompetitiveFormationService.ShadowDefense ownDefense=defenses.ensureShadowDefense(playerId,seasonId);
        long rating=seasons.ensureShadowProfile(playerId,rosterJson(ownDefense.heroes()));
        List<ShadowOpponentView> opponents=new ArrayList<>();
        List<PlayerRow> candidates=jdbc.query("""
                select p.id,p.display_name from players p where p.id<>?
                  and (select count(*) from player_heroes ph where ph.player_id=p.id)>=?
                order by p.created_at,p.id limit 12
                """,(rs,row)->new PlayerRow(rs.getObject("id",UUID.class),rs.getString("display_name")),playerId,BattleRules.SHADOW_ROSTER_SIZE);
        for(PlayerRow candidate:candidates){
            CompetitiveFormationService.ShadowDefense defense=defenses.ensureShadowDefense(candidate.playerId(),seasonId);
            if(defense.heroes().size()!=BattleRules.SHADOW_ROSTER_SIZE) continue;
            long opponentRating=seasons.ensureShadowProfile(candidate.playerId(),rosterJson(defense.heroes()));
            opponents.add(new ShadowOpponentView(candidate.playerId(),candidate.displayName(),opponentRating,totalPower(defense.heroes()),false));
            if(opponents.size()==5) break;
        }
        if(opponents.isEmpty()) opponents.add(new ShadowOpponentView(playerId,"Training Mirror",rating,totalPower(ownDefense.heroes()),true));
        return new ShadowArenaState(seasonId,seasons.currentSeasonEndsAt(),true,ownedCount,BattleRules.SHADOW_ROSTER_SIZE,0,rating,
                ArenaRatingProfile.experimentalV1().version(),SERIES_RULES_VERSION,REWARD_PROFILE_VERSION,
                ownDefense.heroes().size()==BattleRules.SHADOW_ROSTER_SIZE,
                seasons.previousReward(playerId,CompetitiveSeasonService.Mode.SHADOW_ARENA),List.copyOf(opponents));
    }

    @Transactional
    public ShadowDefenseView saveDefense(UUID playerId,List<UUID> heroIds){
        players.require(playerId); String seasonId=seasons.currentSeasonId(CompetitiveSeasonService.Mode.SHADOW_ARENA);
        CompetitiveFormationService.ShadowDefense saved=defenses.saveShadowDefense(playerId,seasonId,heroIds);
        seasons.ensureShadowProfile(playerId,rosterJson(saved.heroes()));
        return new ShadowDefenseView(seasonId,saved.heroes());
    }

    @Transactional
    public ShadowArenaBattleView fight(UUID playerId,UUID opponentPlayerId,UUID requestId){
        if(opponentPlayerId==null) throw new IllegalArgumentException("opponentPlayerId is required");
        if(requestId==null) throw new IllegalArgumentException("requestId is required");
        players.require(playerId); players.require(opponentPlayerId);
        long lockKey=requestId.getMostSignificantBits()^requestId.getLeastSignificantBits()^0x534841444f57L;
        jdbc.query("select pg_advisory_xact_lock(?)",rs->{},lockKey);
        ShadowArenaBattleView replay=loadRequest(requestId);
        if(replay!=null){
            if(!replay.playerId().equals(playerId)||!replay.opponentPlayerId().equals(opponentPlayerId))
                throw new IllegalStateException("Shadow Arena requestId already belongs to another battle");
            return replay.withReplayed(true);
        }
        String seasonId=seasons.currentSeasonId(CompetitiveSeasonService.Mode.SHADOW_ARENA);
        CompetitiveFormationService.ShadowDefense playerDefense=defenses.ensureShadowDefense(playerId,seasonId);
        CompetitiveFormationService.ShadowDefense opponentDefense=defenses.ensureShadowDefense(opponentPlayerId,seasonId);
        if(playerDefense.heroes().size()!=BattleRules.SHADOW_ROSTER_SIZE) throw new IllegalStateException("Shadow Arena requires 15-ninja defense formation");
        if(opponentDefense.heroes().size()!=BattleRules.SHADOW_ROSTER_SIZE) throw new IllegalStateException("Shadow Arena opponent requires 15-ninja defense formation");
        boolean training=playerId.equals(opponentPlayerId);
        long ratingBefore=seasons.ensureShadowProfile(playerId,rosterJson(playerDefense.heroes()));
        long opponentRating=training?ratingBefore:seasons.ensureShadowProfile(opponentPlayerId,rosterJson(opponentDefense.heroes()));
        long masterSeed=secureRandom.nextLong(); SplittableRandom seeds=new SplittableRandom(masterSeed); BattleRuleset ruleset=BattleRuleset.experimentalV1();
        List<ShadowSquadBattleView> squads=new ArrayList<>(); int wins=0,losses=0;
        for(int squadIndex=0;squadIndex<BattleRules.SHADOW_SQUAD_COUNT&&wins<2&&losses<2;squadIndex++){
            List<OwnedHeroView> playerSquad=playerDefense.heroes().subList(squadIndex*5,squadIndex*5+5);
            List<OwnedHeroView> opponentSquad=opponentDefense.heroes().subList(squadIndex*5,squadIndex*5+5);
            long seed=seeds.nextLong(); SquadBuild build=buildSquad(playerSquad,opponentSquad,squadIndex);
            BattleResult battle=engine.simulate(new RealtimeBattleRequest(seed,ruleset,build.units()));
            SquadDecision decision=decide(battle,build.participants(),playerSquad,opponentSquad);
            if(decision.playerWon()) wins++; else losses++;
            squads.add(new ShadowSquadBattleView(squadIndex+1,seed,decision.playerWon(),decision.tiebreak(),build.participants(),battle));
        }
        boolean playerWon=wins>losses; String winner=playerWon?"PLAYER":"OPPONENT";
        ArenaRatingProfile ratingProfile=ArenaRatingProfile.experimentalV1();
        ArenaRatingCalculator.RatingResult rating=training?new ArenaRatingCalculator.RatingResult(ratingBefore,ratingBefore,0,ratingProfile.version())
                :ArenaRatingCalculator.resolve(ratingBefore,playerWon,ratingProfile);
        if(!training) jdbc.update("update shadow_arena_profiles set rating=?, roster_snapshot=cast(? as jsonb), updated_at=? where player_id=? and season_id=?",
                rating.after(),rosterJson(playerDefense.heroes()),clock.instant(),playerId,seasonId);
        UUID battleId=UUID.randomUUID(); long coins=training?0L:playerWon?WIN_COINS:LOSS_COINS;
        String rewardKey=coins>0?"shadow:"+requestId+":coin":null;
        if(coins>0) wallet.mutate(playerId,Currency.SHADOW_COIN,coins,"SHADOW_ARENA_REWARD",seasonId,rewardKey);
        String squadJson=json(Map.of("masterSeed",masterSeed,"ratingBefore",rating.before(),"ratingAfter",rating.after(),"squads",squads));
        jdbc.update("""
                insert into shadow_arena_battles(id,challenger_id,opponent_id,season_id,squad_results,winner,reward_grant_key,created_at)
                values (?,?,?,?,cast(? as jsonb),?,?,?)
                """,battleId,playerId,opponentPlayerId,seasonId,squadJson,winner,rewardKey,clock.instant());
        ShadowArenaBattleView result=new ShadowArenaBattleView(requestId,battleId,playerId,seasonId,false,training,opponentPlayerId,
                opponentRating,totalPower(opponentDefense.heroes()),masterSeed,winner,rating.before(),rating.after(),rating.delta(),
                ratingProfile.version(),SERIES_RULES_VERSION,coins,REWARD_PROFILE_VERSION,ExperimentalCombatStatsResolver.VERSION,
                ExperimentalAbilityProfile.VERSION,TechniqueEffectResolver.VERSION,PassiveEffectResolver.VERSION,List.copyOf(squads));
        persistRequest(requestId,playerId,opponentPlayerId,seasonId,result); return result;
    }

    @Transactional(readOnly=true)
    public List<HistoryItem> history(UUID playerId,int limit){
        players.require(playerId); int safe=Math.max(1,Math.min(50,limit));
        return jdbc.query("""
                select sab.id,sab.challenger_id,other.display_name as other_name,sab.season_id,sab.winner,sab.created_at
                from shadow_arena_battles sab
                join players other on other.id=case when sab.challenger_id=? then sab.opponent_id else sab.challenger_id end
                where sab.challenger_id=? or sab.opponent_id=? order by sab.created_at desc limit ?
                """,(rs,row)->new HistoryItem(rs.getObject("id",UUID.class),rs.getObject("challenger_id",UUID.class).equals(playerId)?"ATTACK":"DEFENSE",
                rs.getString("other_name"),rs.getString("season_id"),rs.getString("winner"),rs.getTimestamp("created_at").toInstant()),
                playerId,playerId,playerId,safe);
    }

    public CompetitiveSeasonService.SeasonRewardState claimPreviousSeason(UUID playerId){
        players.require(playerId); String seasonId=seasons.currentSeasonId(CompetitiveSeasonService.Mode.SHADOW_ARENA);
        CompetitiveFormationService.ShadowDefense defense=defenses.ensureShadowDefense(playerId,seasonId);
        if(defense.heroes().size()==BattleRules.SHADOW_ROSTER_SIZE) seasons.ensureShadowProfile(playerId,rosterJson(defense.heroes()));
        return seasons.claimPrevious(playerId,CompetitiveSeasonService.Mode.SHADOW_ARENA);
    }

    private SquadBuild buildSquad(List<OwnedHeroView> player,List<OwnedHeroView> opponent,int squadIndex){
        List<BattleUnitSeed> units=new ArrayList<>();List<BattleParticipant> participants=new ArrayList<>();
        addHeroes(player,TeamSide.A,"shadow:A:"+squadIndex+":",units,participants);addHeroes(opponent,TeamSide.B,"shadow:B:"+squadIndex+":",units,participants);
        return new SquadBuild(List.copyOf(units),List.copyOf(participants));
    }
    private void addHeroes(List<OwnedHeroView> heroes,TeamSide side,String prefix,List<BattleUnitSeed> units,List<BattleParticipant> participants){
        for(int slot=0;slot<heroes.size();slot++){OwnedHeroView h=heroes.get(slot);BattleUnitSeed u=stats.resolve(prefix+h.id(),h.heroId(),h.awakened(),h.level(),side,slot);
            units.add(u);participants.add(BattleParticipant.heroVersion(u.id(),h.characterId(),h.heroId(),h.awakened(),h.awakeningId(),h.displayName(),h.level(),u.side(),u.slot(),u.maxHp()));}
    }
    private static SquadDecision decide(BattleResult battle,List<BattleParticipant> participants,List<OwnedHeroView> player,List<OwnedHeroView> opponent){
        if(battle.outcome()==BattleOutcome.TEAM_A)return new SquadDecision(true,"NONE");if(battle.outcome()==BattleOutcome.TEAM_B)return new SquadDecision(false,"NONE");
        long playerHp=remainingHp(battle,participants,TeamSide.A),opponentHp=remainingHp(battle,participants,TeamSide.B);
        if(playerHp!=opponentHp)return new SquadDecision(playerHp>opponentHp,"TOTAL_HP");long pp=totalPower(player),op=totalPower(opponent);
        if(pp!=op)return new SquadDecision(pp>op,"SQUAD_POWER");return new SquadDecision(true,"PLAYER_SEED_ORDER");
    }
    private static long remainingHp(BattleResult battle,List<BattleParticipant> participants,TeamSide side){return participants.stream().filter(p->p.side()==side).mapToLong(p->battle.finalHp().getOrDefault(p.battleUnitId(),0L)).sum();}
    private static long totalPower(List<OwnedHeroView> heroes){return heroes.stream().mapToLong(h->h.level()*1_000L+(h.awakened()?250L:0L)+500L).sum();}
    private String rosterJson(List<OwnedHeroView> heroes){return json(Map.of("heroIds",heroes.stream().map(h->h.id().toString()).toList()));}
    private String json(Object value){try{return objectMapper.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("cannot encode Shadow Arena JSON",e);}}
    private ShadowArenaBattleView loadRequest(UUID requestId){List<String> rows=jdbc.query("select result_json from competitive_battle_requests where request_id=? and mode='SHADOW_ARENA'",(rs,row)->rs.getString(1),requestId);if(rows.isEmpty())return null;try{return objectMapper.readValue(rows.get(0),ShadowArenaBattleView.class);}catch(JsonProcessingException e){throw new IllegalStateException("cannot decode Shadow Arena request",e);}}
    private void persistRequest(UUID requestId,UUID playerId,UUID opponentId,String seasonId,ShadowArenaBattleView value){jdbc.update("insert into competitive_battle_requests(request_id,mode,player_id,opponent_player_id,season_id,result_json) values (?,'SHADOW_ARENA',?,?,?,?)",requestId,playerId,opponentId,seasonId,json(value));}

    private record PlayerRow(UUID playerId,String displayName){} private record SquadBuild(List<BattleUnitSeed> units,List<BattleParticipant> participants){} private record SquadDecision(boolean playerWon,String tiebreak){}
    public record ShadowOpponentView(UUID playerId,String displayName,long rating,long totalPower,boolean training){}
    public record ShadowArenaState(String seasonId,Instant seasonEndsAt,boolean eligible,int ownedCount,int requiredCount,int missingCount,long rating,
                                   String ratingProfileVersion,String seriesRulesVersion,String rewardProfileVersion,boolean defenseConfigured,
                                   CompetitiveSeasonService.SeasonRewardState previousSeasonReward,List<ShadowOpponentView> opponents){}
    public record ShadowDefenseView(String seasonId,List<OwnedHeroView> heroes){}
    public record ShadowSquadBattleView(int squadIndex,long seed,boolean playerWon,String tiebreak,List<BattleParticipant> participants,BattleResult battle){}
    public record ShadowArenaBattleView(UUID requestId,UUID battleId,UUID playerId,String seasonId,boolean replayed,boolean training,UUID opponentPlayerId,
                                        long opponentRating,long opponentPower,long masterSeed,String winner,long ratingBefore,long ratingAfter,long ratingDelta,
                                        String ratingProfileVersion,String seriesRulesVersion,long shadowCoinReward,String rewardProfileVersion,
                                        String combatStatsVersion,String abilityProfileVersion,String techniqueMappingVersion,String passiveProfileVersion,
                                        List<ShadowSquadBattleView> squads){
        public ShadowArenaBattleView withReplayed(boolean value){return new ShadowArenaBattleView(requestId,battleId,playerId,seasonId,value,training,opponentPlayerId,
                opponentRating,opponentPower,masterSeed,winner,ratingBefore,ratingAfter,ratingDelta,ratingProfileVersion,seriesRulesVersion,shadowCoinReward,
                rewardProfileVersion,combatStatsVersion,abilityProfileVersion,techniqueMappingVersion,passiveProfileVersion,squads);}}
    public record HistoryItem(UUID battleId,String role,String opponentDisplayName,String seasonId,String winner,Instant createdAt){}
}
