package com.ninjaassemble.quest.application;

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
public final class QuestCatalogService {
    public static final String VERSION = "daily-quest-design-v1";
    private static final String RESOURCE = "/game-data/quest/daily-quests.csv";
    private final List<QuestDefinition> quests;
    private final Map<String, QuestDefinition> byId;

    public QuestCatalogService(ItemCatalogService items) {
        List<QuestDefinition> loaded = new ArrayList<>();
        Map<String, QuestDefinition> index = new LinkedHashMap<>();
        try (InputStream input = QuestCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing quest catalog: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                boolean header = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length != 10) throw new IllegalStateException("invalid quest row: " + line);
                    if (!"DESIGN_BASELINE".equals(cells[9])) throw new IllegalStateException("quest must remain DESIGN_BASELINE: " + cells[0]);
                    Metric metric = Metric.valueOf(cells[3]);
                    long target = Long.parseLong(cells[4]);
                    long gold = Long.parseLong(cells[5]);
                    long diamond = Long.parseLong(cells[6]);
                    String itemId = cells[7].isBlank() ? null : cells[7];
                    long itemQuantity = Long.parseLong(cells[8]);
                    if (target <= 0 || gold < 0 || diamond < 0 || itemQuantity < 0) throw new IllegalStateException("invalid quest values: " + cells[0]);
                    if (itemId != null) items.require(itemId);
                    if (itemId == null && itemQuantity != 0) throw new IllegalStateException("quest item quantity without item: " + cells[0]);
                    QuestDefinition quest = new QuestDefinition(cells[0], cells[1], cells[2], metric, target, gold, diamond, itemId, itemQuantity);
                    if (index.putIfAbsent(quest.id(), quest) != null) throw new IllegalStateException("duplicate quest: " + quest.id());
                    loaded.add(quest);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load quest catalog", exception);
        }
        quests = List.copyOf(loaded);
        byId = Map.copyOf(index);
    }

    public List<QuestDefinition> all() { return quests; }
    public QuestDefinition require(String id) {
        QuestDefinition value = byId.get(id);
        if (value == null) throw new IllegalArgumentException("unknown quest: " + id);
        return value;
    }

    public enum Metric { CAMPAIGN_CLEAR, ARENA_BATTLE, SUMMON, HERO_LEVEL_UP }
    public record QuestDefinition(String id, String nameEn, String nameVi, Metric metric, long target,
                                  long rewardGold, long rewardDiamond, String rewardItemId, long rewardItemQuantity) {}
}
