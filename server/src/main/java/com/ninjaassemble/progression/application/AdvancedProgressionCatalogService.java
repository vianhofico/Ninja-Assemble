package com.ninjaassemble.progression.application;

import com.ninjaassemble.inventory.application.ItemCatalogService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public final class AdvancedProgressionCatalogService {
    public static final String VERSION = "advanced-progression-v1";
    public static final int RELEASE_TRACK_COUNT = 11;
    private static final String RESOURCE = "/game-data/progression/advanced-tracks.csv";
    private final List<TrackDefinition> tracks;
    private final Map<String, TrackDefinition> byId;

    public AdvancedProgressionCatalogService(ItemCatalogService items) {
        List<TrackDefinition> loaded = new ArrayList<>(); Map<String, TrackDefinition> index = new LinkedHashMap<>();
        try (InputStream input = AdvancedProgressionCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing advanced progression catalog");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                boolean header = true; String line;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] c = line.split(",", -1); if (c.length != 14) throw new IllegalStateException("invalid advanced progression row");
                    TrackDefinition t = new TrackDefinition(c[0], TrackType.valueOf(c[1]), c[2], c[3], i(c[4]), i(c[5]), l(c[6]), l(c[7]), c[8], l(c[9]), l(c[10]), c[11], i(c[12]), c[13]);
                    if (!"PRODUCTION_READY".equals(t.releaseStatus())) throw new IllegalStateException("track not release-ready: " + t.id());
                    if (t.maxLevel() <= 0 || t.minPlayerLevel() <= 0 || t.goldBase() < 0 || t.goldGrowth() < 0 || t.itemBase() < 0 || t.itemGrowth() < 0 || t.bonusPerLevel() <= 0 || t.bonusStat().isBlank()) throw new IllegalStateException("invalid advanced progression contract: " + t.id());
                    if (!t.itemId().isBlank()) items.require(t.itemId());
                    if (index.putIfAbsent(t.id(), t) != null) throw new IllegalStateException("duplicate advanced track: " + t.id());
                    loaded.add(t);
                }
            }
        } catch (IOException error) { throw new IllegalStateException("cannot load advanced progression catalog", error); }
        long learning = loaded.stream().filter(t -> t.type() != TrackType.JINCHURIKI).count();
        long beasts = loaded.stream().filter(t -> t.type() == TrackType.JINCHURIKI).count();
        if (loaded.size() != RELEASE_TRACK_COUNT || learning != 2 || beasts != 9) throw new IllegalStateException("advanced progression release census must be 2 learning + 9 Jinchuriki tracks");
        tracks = List.copyOf(loaded); byId = Map.copyOf(index);
    }

    public List<TrackDefinition> all() { return tracks; }
    public TrackDefinition require(String id) { TrackDefinition t = byId.get(id); if (t == null) throw new IllegalArgumentException("unknown progression track: " + id); return t; }
    public record TrackDefinition(String id, TrackType type, String nameEn, String nameVi, int maxLevel, int minPlayerLevel, long goldBase, long goldGrowth, String itemId, long itemBase, long itemGrowth, String bonusStat, int bonusPerLevel, String releaseStatus) {
        public long goldCost(int currentLevel) { return goldBase + goldGrowth * Math.max(0, currentLevel); }
        public long itemCost(int currentLevel) { return itemId.isBlank() ? 0L : itemBase + itemGrowth * Math.max(0, currentLevel); }
        public int cumulativeBonus(int level) { return Math.max(0, level) * bonusPerLevel; }
    }
    public enum TrackType { SCROLL_MASTERY, NINJA_COLLEGE, JINCHURIKI }
    private static int i(String value) { return Integer.parseInt(value); } private static long l(String value) { return Long.parseLong(value); }
}
