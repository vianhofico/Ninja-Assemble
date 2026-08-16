package com.ninjaassemble.hero.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HeroCatalogService {
    private static final String ROSTER_RESOURCE = "/game-data/reference/roster-complete.csv";
    private final List<HeroCatalogEntry> entries;

    public HeroCatalogService() {
        entries = List.copyOf(load());
    }

    private static List<HeroCatalogEntry> load() {
        try (InputStream input = HeroCatalogService.class.getResourceAsStream(ROSTER_RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing packaged Complete Roster+: " + ROSTER_RESOURCE);
            List<HeroCatalogEntry> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length < 4) throw new IllegalStateException("invalid roster row: " + line);
                    loaded.add(new HeroCatalogEntry(cells[0].trim(), cells[1].trim(), cells[2].trim(), cells[3].trim()));
                }
            }
            return loaded;
        } catch (IOException e) {
            throw new IllegalStateException("cannot load Complete Roster+", e);
        }
    }

    public List<HeroCatalogEntry> all() { return entries; }

    public List<HeroCatalogEntry> byGroup(String group) {
        if (group == null || group.isBlank()) return entries;
        return entries.stream().filter(it -> it.group().equalsIgnoreCase(group)).toList();
    }

    public HeroCatalogEntry require(String id) {
        return entries.stream().filter(it -> it.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown hero: " + id));
    }
}
