package com.ninjaassemble.play.application;

import com.ninjaassemble.battle.sim.BattleRequest;
import com.ninjaassemble.battle.sim.BattleResult;
import com.ninjaassemble.battle.sim.BattleRuleset;
import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.DeterministicBattleEngine;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.player.application.EnergyService;
import com.ninjaassemble.player.application.PlayerService;
import com.ninjaassemble.play.domain.BattleParticipant;
import com.ninjaassemble.play.domain.ExperimentalAbilityProfile;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
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
    private static final int ENERGY_COST = 5;
    private static final long GOLD_REWARD = 500;
    private final PlayerService players;
    private final FormationService formations;
    private final EnergyService energy;
    private final WalletService wallet;
    private final ExperimentalCombatStatsResolver stats;
    private final VerticalEnemyTeamService enemies;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final DeterministicBattleEngine engine = new DeterministicBattleEngine();

    public PlayableBattleService(PlayerService players, FormationService formations, EnergyService energy, WalletService wallet,
                                 ExperimentalCombatStatsResolver stats, VerticalEnemyTeamService enemies, JdbcTemplate jdbc, Clock clock) {
        this.players = players; this.formations = formations; this.energy = energy; this.wallet = wallet;
        this.stats = stats; this.enemies = enemies; this.jdbc = jdbc; this.clock = clock;
    }

    @Transactional
    public PlayBattleResult play(UUID playerId) {
        players.require(playerId);
        FormationService.FormationView formation = formations.load(playerId);
        if (formation.heroes().size() != 5) throw new IllegalStateException("save a five-ninja campaign formation before battle");
        energy.spend(playerId, ENERGY_COST);

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

        for (VerticalEnemyTeamService.EnemyBattleEntry enemy : enemies.battleEntries()) {
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
        long gold = result.outcome() == com.ninjaassemble.battle.sim.BattleOutcome.TEAM_A ? GOLD_REWARD : 0;
        String rewardKey = gold > 0 ? "battle:" + battleId + ":gold" : null;
        if (gold > 0) wallet.mutate(playerId, Currency.GOLD, gold, "CAMPAIGN_BATTLE_REWARD", "vertical-1", rewardKey);
        jdbc.update("""
                insert into campaign_runs(id, player_id, stage_id, battle_seed, ruleset_version, result, reward_grant_key, started_at, completed_at)
                values (?, ?, 'vertical-1', ?, ?, ?, ?, ?, ?)
                """, battleId, playerId, seed, ruleset.version(), result.outcome().name(), rewardKey, clock.instant(), clock.instant());
        return new PlayBattleResult(
                battleId,
                ENERGY_COST,
                gold,
                ExperimentalCombatStatsResolver.VERSION,
                ExperimentalAbilityProfile.VERSION,
                TechniqueEffectResolver.VERSION,
                List.copyOf(participants),
                result);
    }

    public record PlayBattleResult(
            UUID battleId,
            int energyCost,
            long goldReward,
            String combatStatsVersion,
            String abilityProfileVersion,
            String techniqueMappingVersion,
            List<BattleParticipant> participants,
            BattleResult battle
    ) {}
}
