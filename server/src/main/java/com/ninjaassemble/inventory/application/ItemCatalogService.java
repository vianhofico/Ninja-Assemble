package com.ninjaassemble.inventory.application;

import com.ninjaassemble.inventory.domain.ItemType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public final class ItemCatalogService {
    public static final String VERSION = "item-catalog-v1";
    private static final String RESOURCE = "/game-data/items/item-definitions.csv";
    private final Map<String, ItemDefinition> definitions;

    public ItemCatalogService() {
        Map<String, ItemDefinition> loaded = new LinkedHashMap<>();
        try (InputStream input = ItemCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing item definitions: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                boolean header = true;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length != 5) throw new IllegalStateException("invalid item definition row: " + line);
                    ItemDefinition definition = new ItemDefinition(cells[0], ItemType.valueOf(cells[1]), cells[2], cells[3], cells[4]);
                    if (loaded.putIfAbsent(definition.id(), definition) != null) throw new IllegalStateException("duplicate item definition: " + definition.id());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("cannot load item definitions", exception);
        }
        definitions = Map.copyOf(loaded);
    }

    public ItemDefinition require(String id) {
        ItemDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("unknown item definition: " + id);
        return definition;
    }

    public List<ItemDefinition> all() { return List.copyOf(definitions.values()); }
    public int size() { return definitions.size(); }

    public record ItemDefinition(String id, ItemType type, String nameEn, String nameVi, String status) {
        public ItemDefinition {
            if (id == null || id.isBlank() || type == null || nameEn == null || nameEn.isBlank()) throw new IllegalArgumentException("item identity required");
        }
    }
}
