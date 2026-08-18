package com.ninjaassemble.pve.application;

import com.ninjaassemble.hero.catalog.HeroCatalogService;
import com.ninjaassemble.inventory.application.ItemCatalogService;
import com.ninjaassemble.pve.domain.PveModeDefinition;
import com.ninjaassemble.pve.domain.PveModeType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public final class ResourcePveCatalogService {
    public static final String VERSION = "resource-pve-catalog-v1";
    private static final String MODES = "/game-data/pve/resource-modes.csv";
    private static final String ENEMIES = "/game-data/pve/resource-mode-enemies.csv";
    public static final int RELEASE_MODE_COUNT = 9;

    private final List<ModeEntry> modes;
    private final Map<String, ModeEntry> byId;

    public ResourcePveCatalogService(HeroCatalogService heroes, ItemCatalogService items) {
        Map<String, ModeMetadata> metadata = new LinkedHashMap<>();
        forEachRow(MODES, cells -> {
            if (cells.length != 13) throw new IllegalStateException("invalid resource PvE mode row");
            ModeMetadata row = new ModeMetadata(
                    cells[0], PveModeType.valueOf(cells[1]), cells[2], cells[3], integer(cells[4]), integer(cells[5]),
                    integer(cells[6]), integer(cells[7]), longValue(cells[8]), cells[9], longValueOrZero(cells[10]),
                    cells[11], cells[12]);
            if (!row.rewardItemId.isBlank()) items.require(row.rewardItemId);
            if (!"DAILY_UTC".equals(row.resetPolicy)) throw new IllegalStateException("unsupported PvE reset policy: " + row.resetPolicy);
            if (!"PRODUCTION_READY".equals(row.releaseStatus)) throw new IllegalStateException("resource PvE mode is not release-ready: " + row.modeId);
            if (metadata.putIfAbsent(row.modeId, row) != null) throw new IllegalStateException("duplicate PvE mode: " + row.modeId);
        });
        if (metadata.size() != RELEASE_MODE_COUNT) throw new IllegalStateException("resource PvE release census must contain nine modes");

        Map<String, List<EnemySpec>> enemyByMode = new LinkedHashMap<>();
        forEachRow(ENEMIES, cells -> {
            if (cells.length != 5) throw new IllegalStateException("invalid resource PvE enemy row");
            String modeId = cells[0];
            if (!metadata.containsKey(modeId)) throw new IllegalStateException("PvE enemy references unknown mode: " + modeId);
            heroes.require(cells[2]);
            EnemySpec enemy = new EnemySpec(integer(cells[1]), cells[2], cells[3].isBlank() ? null : cells[3], integer(cells[4]));
            enemyByMode.computeIfAbsent(modeId, ignored -> new ArrayList<>()).add(enemy);
        });

        List<ModeEntry> loaded = new ArrayList<>();
        Map<String, ModeEntry> index = new LinkedHashMap<>();
        for (ModeMetadata row : metadata.values()) {
            List<EnemySpec> enemies = enemyByMode.getOrDefault(row.modeId, List.of()).stream()
                    .sorted(java.util.Comparator.comparingInt(EnemySpec::slot)).toList();
            if (enemies.size() != row.teamSize) throw new IllegalStateException("PvE enemy team size mismatch: " + row.modeId);
            for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).slot() != i) throw new IllegalStateException("PvE enemy slots must be contiguous: " + row.modeId);
            PveModeDefinition definition = new PveModeDefinition(
                    row.modeId, row.modeType, row.teamSize, row.energyCost, row.dailyAttemptLimit, Set.of(), row.modeId + "-reward-v1");
            ModeEntry entry = new ModeEntry(definition, row.nameEn, row.nameVi, row.minPlayerLevel, row.rewardGold,
                    row.rewardItemId.isBlank() ? null : row.rewardItemId, row.rewardItemQuantity, row.resetPolicy, row.releaseStatus, enemies);
            loaded.add(entry); index.put(row.modeId, entry);
        }
        modes = List.copyOf(loaded); byId = Map.copyOf(index);
    }

    public List<ModeEntry> all() { return modes; }
    public ModeEntry require(String modeId) {
        ModeEntry value = byId.get(modeId);
        if (value == null) throw new IllegalArgumentException("unknown resource PvE mode: " + modeId);
        return value;
    }

    private static void forEachRow(String resource, RowConsumer consumer) {
        try (InputStream input = ResourcePveCatalogService.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("missing resource PvE data: " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                boolean header = true; String line;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (!line.isBlank()) consumer.accept(line.split(",", -1));
                }
            }
        } catch (IOException error) { throw new IllegalStateException("cannot read resource PvE data: " + resource, error); }
    }
    private static int integer(String value) { return Integer.parseInt(value); }
    private static long longValue(String value) { return Long.parseLong(value); }
    private static long longValueOrZero(String value) { return value == null || value.isBlank() ? 0L : Long.parseLong(value); }
    @FunctionalInterface private interface RowConsumer { void accept(String[] cells); }

    private record ModeMetadata(String modeId, PveModeType modeType, String nameEn, String nameVi, int teamSize,
                                int energyCost, int dailyAttemptLimit, int minPlayerLevel, long rewardGold,
                                String rewardItemId, long rewardItemQuantity, String resetPolicy, String releaseStatus) { }
    public record EnemySpec(int slot, String characterId, String variant, int level) { }
    public record ModeEntry(PveModeDefinition definition, String nameEn, String nameVi, int minPlayerLevel,
                            long rewardGold, String rewardItemId, long rewardItemQuantity,
                            String resetPolicy, String releaseStatus, List<EnemySpec> enemies) { }
}
