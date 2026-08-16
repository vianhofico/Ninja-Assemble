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
import com.ninjaassemble.campaign.domain.WaveDefinition;
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
import java.util.SplittableRandom;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayableBattleService {
    public static final String DEFAULT_STAGE_ID = "c1-s1";
    public static final String WAVE_RULES_VERSION = "full-recovery-between-waves-v1";
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

        long masterSeed = secureRandom.nextLong();
        SplittableRandom waveSeedSource = new SplittableRandom(masterSeed);
        BattleRuleset ruleset = BattleRuleset.experimentalV1();
        List<CampaignWaveResult> waveResults = new ArrayList<>();
        int totalRounds = 0;

        for (WaveDefinition wave : stage.waves()) {
            long waveSeed = waveSeedSource.nextLong();
            List<BattleUnitSeed> units = new ArrayList<>();
            List<BattleParticipant> participants = new ArrayList<>();
            addPlayerFormation(formation, units, participants);
            for (CampaignEnemyTeamService.EnemyBattleEntry enemy : campaignEnemies.battleEntries(stage, wave)) {
                BattleUnitSeed unit = enemy.unit();
                units.add(unit);
                participants.add(new BattleParticipant(
                        unit.id(), enemy.characterId(), enemy.displayName(), enemy.variant(), enemy.level(),
                        unit.side(), unit.slot(), unit.maxHp()));
            }
            BattleResult waveBattle = engine.simulate(new BattleRequest(waveSeed, ruleset, units));
            totalRounds += waveBattle.rounds();
            waveResults.add(new CampaignWaveResult(wave.index(), waveSeed, List.copyOf(participants), waveBattle));
            if (waveBattle.outcome() != BattleOutcome.TEAM_A) break;
        }

        CampaignWaveResult lastWave = waveResults.get(waveResults.size() - 1);
        boolean won = waveResults.size() == stage.waves().size() && lastWave.battle().outcome() == BattleOutcome.TEAM_A;
        UUID battleId = UUID.randomUUID();
        int stars = won ? stars(totalRounds, stage.waves().size()) : 0;
        boolean firstClear = false;
        long playerExpReward = 0;
        long goldReward = 0;
        long diamondReward = 0;
        int accountLevelAfter = players.require(playerId).getAccountLevel();
        List<CampaignRewardService.ItemGrant> itemRewards = List.of();
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
            itemRewards = grant.items();
            rewardKey = "campaign:" + battleId;
        }

        jdbc.update("""
                insert into campaign_runs(id, player_id, stage_id, battle_seed, ruleset_version, result, reward_grant_key, started_at, completed_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, battleId, playerId, stage.id(), masterSeed, ruleset.version(), won ? BattleOutcome.TEAM_A.name() : lastWave.battle().outcome().name(),
                rewardKey, clock.instant(), clock.instant());

        return new PlayBattleResult(
                battleId,
                stage.id(),
                CampaignStageCatalogService.VERSION,
                WAVE_RULES_VERSION,
                stage.energyCost(),
                stars,
                firstClear,
                playerExpReward,
                goldReward,
                diamondReward,
                accountLevelAfter,
                List.copyOf(itemRewards),
                ExperimentalCombatStatsResolver.VERSION,
                ExperimentalAbilityProfile.VERSION,
                TechniqueEffectResolver.VERSION,
                PassiveEffectResolver.VERSION,
                List.copyOf(waveResults),
                lastWave.participants(),
                lastWave.battle());
    }

    private void addPlayerFormation(FormationService.FormationView formation, List<BattleUnitSeed> units, List<BattleParticipant> participants) {
        for (int slot = 0; slot < formation.heroes().size(); slot++) {
            OwnedHeroView hero = formation.heroes().get(slot);
            BattleUnitSeed unit = stats.resolve(
                    hero.id().toString(), hero.heroId(), hero.awakened(), hero.level(), TeamSide.A, slot);
            units.add(unit);
            participants.add(new BattleParticipant(
                    unit.id(), hero.characterId(), hero.displayName(),
                    hero.awakened() ? hero.awakeningName() : hero.heroId(), hero.level(),
                    unit.side(), unit.slot(), unit.maxHp()));
        }
    }

    private static int stars(int totalRounds, int waveCount) {
        if (totalRounds <= 5 * waveCount) return 3;
        if (totalRounds <= 10 * waveCount) return 2;
        return 1;
    }

    public record CampaignWaveResult(int waveIndex, long waveSeed, List<BattleParticipant> participants, BattleResult battle) {}

    public record PlayBattleResult(
            UUID battleId,
            String stageId,
            String campaignCatalogVersion,
            String waveRulesVersion,
            int energyCost,
            int stars,
            boolean firstClear,
            long playerExpReward,
            long goldReward,
            long diamondReward,
            int accountLevelAfter,
            List<CampaignRewardService.ItemGrant> itemRewards,
            String combatStatsVersion,
            String abilityProfileVersion,
            String techniqueMappingVersion,
            String passiveProfileVersion,
            List<CampaignWaveResult> waves,
            List<BattleParticipant> participants,
            BattleResult battle
    ) {}
}
