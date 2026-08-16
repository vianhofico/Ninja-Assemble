package com.ninjaassemble.play.application;

import com.ninjaassemble.battle.sim.BattleOutcome;
import com.ninjaassemble.battle.sim.BattleRequest;
import com.ninjaassemble.battle.sim.BattleResult;
import com.ninjaassemble.battle.sim.BattleRuleset;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.DeterministicBattleEngine;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.campaign.application.CampaignEnemyTeamService;
import com.ninjaassemble.campaign.application.CampaignProgressService;
import com.ninjaassemble.campaign.application.CampaignRewardService;
import com.ninjaassemble.campaign.application.CampaignStageCatalogService;
import com.ninjaassemble.campaign.application.CampaignStageFlowService;
import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.campaign.domain.StageDefinition;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.play.domain.BattleParticipant;
import com.ninjaassemble.play.domain.ExperimentalAbilityProfile;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
import com.ninjaassemble.play.domain.PassiveEffectResolver;
import com.ninjaassemble.play.domain.TechniqueEffectResolver;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayableBattleService {
    public static final String DEFAULT_STAGE_ID = "c1-s1";
    private final PlayerService players;
    private final FormationService formations;
    private final EnergyService energy;
    private final ExperimentalCombatStatsResolver stats;
    private final CampaignStageFlowService stageFlow;
    private final CampaignEnemyTeamService campaignEnemies;
    private final CampaignProgressService progress;
    private final CampaignRewardService rewards;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DeterministicBattleEngine engine = new DeterministicBattleEngine();

    public PlayableBattleService(PlayerService players, FormationService formations, EnergyService energy,
                                 ExperimentalCombatStatsResolver stats, CampaignStageFlowService stageFlow,
                                 CampaignEnemyTeamService campaignEnemies, CampaignProgressService progress,
                                 CampaignRewardService rewards, JdbcTemplate jdbc, Clock clock) {
        this.players = players;
        this.formations = formations;
        this.energy = energy;
        this.stats = stats;
        this.stageFlow = stageFlow;
        this.campaignEnemies = campaignEnemies;
        this.progress = progress;
        this.rewards = rewards;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public PlayBattleResult play(UUID playerId) {
        return play(playerId, DEFAULT_STAGE_ID);
    }

    @Transactional
    public PlayBattleResult play(UUID playerId, String stageId) {
        players.require(playerId);
        FormationService.FormationView formation = formations.load(playerId);
        if (formation.heroes().size() != 5) throw new IllegalStateException("save a five-ninja campaign formation before battle");
        CampaignStageCatalogService.CampaignStageEntry stageEntry = stageFlow.requirePlayable(playerId, stageId);
        StageDefinition stage = stageEntry.stage();
        energy.spend(playerId, stage.energyCost());

        List<BattleUnitSeed> units = new ArrayList<>();
        List<BattleParticipant> participants = new ArrayList<>();
        for (int slot = 0; slot < formation.heroes().size(); slot++) {
            OwnedHeroView hero = formation.heroes().get(slot);
            BattleUnitSeed unit = stats.resolve(
                    hero.id().toString(), hero.characterId(), hero.currentVariant(), hero.level(), TeamSide.A, slot);
            units.add(unit);
            participants.add(new BattleParticipant(
                    unit.id(), hero.characterId(), hero.displayName(), hero.currentVariant(), hero.level(),
                    unit.side(), unit.slot(), unit.maxHp()));
        }

        for (CampaignEnemyTeamService.EnemyBattleEntry enemy : campaignEnemies.battleEntries(stage)) {
            BattleUnitSeed unit = enemy.unit();
            units.add(unit);
            participants.add(new BattleParticipant(
                    unit.id(), enemy.characterId(), enemy.displayName(), enemy.variant(), enemy.level(),
                    unit.side(), unit.slot(), unit.maxHp()));
        }

        long seed = secureRandom.nextLong();
        BattleRuleset ruleset = BattleRuleset.experimentalV1();
        BattleResult result = engine.simulate(new BattleRequest(seed, ruleset, units));
        UUID battleId = UUID.randomUUID();
        boolean won = result.outcome() == BattleOutcome.TEAM_A;
        int stars = won ? stars(result) : 0;
        boolean firstClear = false;
        long playerExpReward = 0;
        long goldReward = 0;
        long diamondReward = 0;
        int accountLevelAfter = players.require(playerId).getAccountLevel();
        String rewardKey = null;

        if (won) {
            CampaignProgressService.ClearRecord clear = progress.recordClear(playerId, stage.id(), stars);
            firstClear = clear.firstClear();
            RewardBundle reward = firstClear ? stage.firstClearReward() : stage.repeatReward();
            CampaignRewardService.RewardGrant grant = rewards.grant(playerId, stage.id(), reward, battleId);
            playerExpReward = grant.playerExp();
            goldReward = grant.gold();
            diamondReward = grant.diamond();
            accountLevelAfter = grant.accountLevelAfter();
            rewardKey = "campaign:" + battleId;
        }

        jdbc.update("""
                insert into campaign_runs(id, player_id, stage_id, battle_seed, ruleset_version, result, reward_grant_key, started_at, completed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, battleId, playerId, stage.id(), seed, ruleset.version(), result.outcome().name(), rewardKey, clock.instant(), clock.instant());

        return new PlayBattleResult(
                battleId,
                stage.id(),
                CampaignStageCatalogService.VERSION,
                stage.energyCost(),
                stars,
                firstClear,
                playerExpReward,
                goldReward,
                diamondReward,
                accountLevelAfter,
                ExperimentalCombatStatsResolver.VERSION,
                ExperimentalAbilityProfile.VERSION,
                TechniqueEffectResolver.VERSION,
                PassiveEffectResolver.VERSION,
                List.copyOf(participants),
                result);
    }

    private static int stars(BattleResult result) {
        if (result.rounds() <= 5) return 3;
        if (result.rounds() <= 10) return 2;
        return 1;
    }

    public record PlayBattleResult(
            UUID battleId,
            String stageId,
            String campaignCatalogVersion,
            int energyCost,
            int stars,
            boolean firstClear,
            long playerExpReward,
            long goldReward,
            long diamondReward,
            int accountLevelAfter,
            String combatStatsVersion,
            String abilityProfileVersion,
            String techniqueMappingVersion,
            String passiveProfileVersion,
            List<BattleParticipant> participants,
            BattleResult battle
    ) {}
}
