package com.ninjaassemble.progression.scroll;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayableScrollCatalogService {
    public static final String VERSION = "scroll-expansion-playable-v1";
    private static final String RESOURCE = "/game-data/progression/playable-scrolls.csv";
    private final List<ScrollView> definitions;
    private final JdbcTemplate jdbc;

    public PlayableScrollCatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.definitions = load();
    }

    public List<ScrollView> all() { return definitions; }
    public ScrollView require(String id) {
        return definitions.stream().filter(it -> it.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown scroll: " + id));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncDefinitions() {
        for (ScrollView scroll : definitions) {
            jdbc.update("""
                    insert into scroll_definitions(id, name_key, element, max_level, stats, parity_status)
                    values (?, ?, ?, ?, jsonb_build_object('primaryStat', ?, 'baseValue', ?, 'profileVersion', ?), 'IMPLEMENTED')
                    on conflict (id) do update set name_key = excluded.name_key, element = excluded.element,
                        max_level = excluded.max_level, stats = excluded.stats
                    """, scroll.id(), scroll.nameKey(), scroll.element(), scroll.maxLevel(), scroll.primaryStat(),
                    scroll.baseValue(), VERSION);
        }
    }

    private static List<ScrollView> load() {
        try (InputStream input = PlayableScrollCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing playable scroll catalog: " + RESOURCE);
            List<ScrollView> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line; boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] c = line.split(",", -1);
                    if (c.length != 7) throw new IllegalStateException("invalid scroll row: " + line);
                    loaded.add(new ScrollView(c[0], c[1], c[2], Integer.parseInt(c[3]), c[4], Long.parseLong(c[5]), c[6], VERSION));
                }
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load playable scroll catalog", e);
        }
    }

    public record ScrollView(String id, String nameKey, String element, int maxLevel, String primaryStat,
                             long baseValue, String status, String profileVersion) {}
}
