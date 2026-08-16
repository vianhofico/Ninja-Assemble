package com.ninjaassemble.campaign.application;

import com.ninjaassemble.campaign.domain.CampaignDifficulty;
import com.ninjaassemble.campaign.domain.EnemySlotDefinition;
import com.ninjaassemble.campaign.domain.RewardBundle;
import com.ninjaassemble.campaign.domain.StageDefinition;
import com.ninjaassemble.campaign.domain.WaveDefinition;
import com.ninjaassemble.hero.catalog.HeroCatalogService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public final class CampaignStageCatalogService {
    public static final String VERSION = "campaign-stage-catalog-v1";
    private static final String STAGES = "/game-data/campaign/stages.csv";
    private static final String ENEMIES = "/game-data/campaign/stage-enemies.csv";

    private final List<CampaignStageEntry> entries;
    private final Map<String, CampaignStageEntry> byId;

    public CampaignStageCatalogService(HeroCatalogService heroes) {
        Map<String, Metadata> metadata = loadMetadata();
        Map<String, Map<Integer, List<EnemySlotDefinition>>> enemyGroups = loadEnemies(heroes, metadata.keySet());
        List<CampaignStageEntry> loaded = new ArrayList<>();
        Set<String> chapterIndexes = new HashSet<>();

        for (Metadata row : metadata.values()) {
            String chapterIndex = row.chapter + ":" + row.stageIndex;
            if (!chapterIndexes.add(chapterIndex)) throw new IllegalStateException("duplicate campaign chapter/index: " + chapterIndex);
            Map<Integer, List<EnemySlotDefinition>> wavesByIndex = enemyGroups.getOrDefault(row.stageId, Map.of());
            if (wavesByIndex.isEmpty()) throw new IllegalStateException("campaign stage has no enemy wave: " + row.stageId);
            List<WaveDefinition> waves = wavesByIndex.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> new WaveDefinition(entry.getKey(), entry.getValue()))
                    .toList();
            StageDefinition stage = new StageDefinition(
                    row.stageId,
                    row.chapter,
                    row.stageIndex,
                    CampaignDifficulty.valueOf(row.difficulty),
                    row.energyCost,
                    row.minPlayerLevel,
                    row.prerequisiteStageId.isBlank() ? Set.of() : Set.of(row.prerequisiteStageId),
                    waves,
                    reward(row.firstPlayerExp, row.firstGold, row.firstDiamond),
                    reward(row.repeatPlayerExp, row.repeatGold, row.repeatDiamond));
            loaded.add(new CampaignStageEntry(stage, row.nameEn, row.nameVi));
        }

        loaded.sort(Comparator.comparingInt((CampaignStageEntry it) -> it.stage().chapter())
                .thenComparingInt(it -> it.stage().index()));
        Map<String, CampaignStageEntry> index = new LinkedHashMap<>();
        for (CampaignStageEntry entry : loaded) {
            if (index.putIfAbsent(entry.stage().id(), entry) != null) throw new IllegalStateException("duplicate campaign stage: " + entry.stage().id());
        }
        for (CampaignStageEntry entry : loaded) {
            for (String prerequisite : entry.stage().prerequisiteStageIds()) {
                if (!index.containsKey(prerequisite)) throw new IllegalStateException("unknown campaign prerequisite " + prerequisite + " for " + entry.stage().id());
            }
        }
        entries = List.copyOf(loaded);
        byId = Map.copyOf(index);
    }

    public List<CampaignStageEntry> all() { return entries; }

    public CampaignStageEntry require(String stageId) {
        CampaignStageEntry value = byId.get(stageId);
        if (value == null) throw new IllegalArgumentException("unknown campaign stage: " + stageId);
        return value;
    }

    public int size() { return entries.size(); }

    private static RewardBundle reward(long playerExp, long gold, long diamond) {
        Map<String, Long> currencies = new LinkedHashMap<>();
        if (gold > 0) currencies.put("GOLD", gold);
        if (diamond > 0) currencies.put("DIAMOND", diamond);
        return new RewardBundle(playerExp, currencies, Map.of());
    }

    private static Map<String, Metadata> loadMetadata() {
        Map<String, Metadata> result = new LinkedHashMap<>();
        forEachRow(STAGES, cells -> {
            if (cells.length != 15) throw new IllegalStateException("invalid campaign stage row");
            Metadata value = new Metadata(
                    cells[0], integer(cells[1]), integer(cells[2]), cells[3], integer(cells[4]), integer(cells[5]), cells[6],
                    cells[7], cells[8], longValue(cells[9]), longValue(cells[10]), longValue(cells[11]),
                    longValue(cells[12]), longValue(cells[13]), longValue(cells[14]));
            if (result.putIfAbsent(value.stageId, value) != null) throw new IllegalStateException("duplicate campaign stage metadata: " + value.stageId);
        });
        return result;
    }

    private static Map<String, Map<Integer, List<EnemySlotDefinition>>> loadEnemies(HeroCatalogService heroes, Set<String> stageIds) {
        Map<String, Map<Integer, List<EnemySlotDefinition>>> result = new HashMap<>();
        forEachRow(ENEMIES, cells -> {
            if (cells.length != 6) throw new IllegalStateException("invalid campaign enemy row");
            String stageId = cells[0];
            if (!stageIds.contains(stageId)) throw new IllegalStateException("campaign enemy references unknown stage: " + stageId);
            heroes.require(cells[3]);
            int wave = integer(cells[1]);
            EnemySlotDefinition enemy = new EnemySlotDefinition(integer(cells[2]), cells[3], integer(cells[5]), cells[4].isBlank() ? null : cells[4]);
            result.computeIfAbsent(stageId, ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(wave, ignored -> new ArrayList<>())
                    .add(enemy);
        });
        return result;
    }

    private static void forEachRow(String resource, RowConsumer consumer) {
        try (InputStream input = CampaignStageCatalogService.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("missing campaign resource: " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                boolean header = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    consumer.accept(line.split(",", -1));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read campaign resource: " + resource, exception);
        }
    }

    private static int integer(String value) { return Integer.parseInt(value); }
    private static long longValue(String value) { return Long.parseLong(value); }

    @FunctionalInterface private interface RowConsumer { void accept(String[] cells); }

    private record Metadata(
            String stageId, int chapter, int stageIndex, String difficulty, int energyCost, int minPlayerLevel,
            String prerequisiteStageId, String nameEn, String nameVi,
            long firstPlayerExp, long firstGold, long firstDiamond,
            long repeatPlayerExp, long repeatGold, long repeatDiamond) {}

    public record CampaignStageEntry(StageDefinition stage, String nameEn, String nameVi) {}
}
