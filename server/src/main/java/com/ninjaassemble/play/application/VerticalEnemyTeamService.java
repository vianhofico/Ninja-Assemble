package com.ninjaassemble.play.application;

import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.battle.sim.TeamSide;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.play.domain.ExperimentalCombatStatsResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class VerticalEnemyTeamService {
    private static final String RESOURCE = "/game-data/play/vertical-slice-enemies.csv";
    private final List<Enemy> enemies;
    private final HeroCatalogService catalog;
    private final ExperimentalCombatStatsResolver stats;

    public VerticalEnemyTeamService(HeroCatalogService catalog, ExperimentalCombatStatsResolver stats) {
        this.catalog = catalog;
        this.stats = stats;
        this.enemies = load();
    }

    public List<BattleUnitSeed> battleUnits() {
        List<BattleUnitSeed> result = new ArrayList<>();
        for (Enemy enemy : enemies) {
            catalog.require(enemy.characterId());
            result.add(stats.resolve("enemy:" + enemy.slot() + ":" + enemy.characterId(), enemy.characterId(),
                    enemy.variant().isBlank() ? null : enemy.variant(), enemy.level(), TeamSide.B, enemy.slot()));
        }
        return result;
    }

    private static List<Enemy> load() {
        try (InputStream input = VerticalEnemyTeamService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing enemy team resource: " + RESOURCE);
            List<Enemy> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line; boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length != 4) throw new IllegalStateException("invalid enemy row: " + line);
                    loaded.add(new Enemy(Integer.parseInt(cells[0]), cells[1], cells[2], Integer.parseInt(cells[3])));
                }
            }
            if (loaded.size() != 5) throw new IllegalStateException("vertical slice enemy team must contain exactly five units");
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load vertical slice enemy team", e);
        }
    }

    private record Enemy(int slot, String characterId, String variant, int level) {}
}
