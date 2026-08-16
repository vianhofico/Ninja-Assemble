package com.ninjaassemble.campaign.application;

import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.campaign.domain.EnemySlotDefinition;
import com.ninjaassemble.campaign.domain.StageDefinition;
import com.ninjaassemble.campaign.domain.WaveDefinition;
import com.ninjaassemble.hero.catalog.HeroCatalogEntry;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public final class CampaignEnemyTeamService {
    private final HeroCatalogService heroes;
    private final ExperimentalCombatStatsResolver stats;

    public CampaignEnemyTeamService(HeroCatalogService heroes, ExperimentalCombatStatsResolver stats) {
        this.heroes = heroes;
        this.stats = stats;
    }

    public List<EnemyBattleEntry> battleEntries(StageDefinition stage, WaveDefinition wave) {
        if (stage == null || wave == null || !stage.waves().contains(wave)) throw new IllegalArgumentException("wave must belong to stage");
        List<EnemyBattleEntry> result = new ArrayList<>();
        for (EnemySlotDefinition enemy : wave.enemies()) {
            HeroCatalogEntry hero = heroes.require(enemy.enemyDefinitionId());
            String variant = enemy.variantId() == null || enemy.variantId().isBlank() ? null : enemy.variantId();
            BattleUnitSeed unit = stats.resolve(
                    "enemy:" + stage.id() + ":w" + wave.index() + ":s" + enemy.slot() + ":" + enemy.enemyDefinitionId(),
                    enemy.enemyDefinitionId(), variant, enemy.level(), TeamSide.B, enemy.slot());
            result.add(new EnemyBattleEntry(unit, enemy.enemyDefinitionId(), hero.character(), variant, enemy.level()));
        }
        if (result.size() != 5) throw new IllegalStateException("playable campaign wave requires five enemies: " + stage.id() + ":w" + wave.index());
        return List.copyOf(result);
    }

    public record EnemyBattleEntry(BattleUnitSeed unit, String characterId, String displayName, String variant, int level) {}
}
