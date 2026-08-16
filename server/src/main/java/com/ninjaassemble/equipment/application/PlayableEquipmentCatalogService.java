package com.ninjaassemble.equipment.application;

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
public class PlayableEquipmentCatalogService {
    public static final String VERSION = "equipment-expansion-playable-v1";
    private static final String RESOURCE = "/game-data/equipment/playable-equipment.csv";
    private final List<EquipmentView> definitions;
    private final JdbcTemplate jdbc;

    public PlayableEquipmentCatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.definitions = load();
    }

    public List<EquipmentView> all() { return definitions; }

    public EquipmentView require(String id) {
        return definitions.stream().filter(it -> it.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown equipment: " + id));
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncDefinitions() {
        for (EquipmentView item : definitions) {
            jdbc.update("""
                    insert into equipment_definitions(id, name_key, slot, rarity, max_enhance_level, set_id, content, parity_status)
                    values (?, ?, ?, ?, ?, ?, jsonb_build_object('attack', ?, 'hp', ?, 'defense', ?, 'profileVersion', ?), 'IMPLEMENTED')
                    on conflict (id) do update set name_key = excluded.name_key, slot = excluded.slot, rarity = excluded.rarity,
                        max_enhance_level = excluded.max_enhance_level, set_id = excluded.set_id, content = excluded.content
                    """, item.id(), item.nameKey(), item.slot(), item.rarity(), item.maxEnhanceLevel(), item.setId(),
                    item.attack(), item.hp(), item.defense(), VERSION);
        }
    }

    private static List<EquipmentView> load() {
        try (InputStream input = PlayableEquipmentCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing playable equipment catalog: " + RESOURCE);
            List<EquipmentView> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line; boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] c = line.split(",", -1);
                    if (c.length != 10) throw new IllegalStateException("invalid equipment row: " + line);
                    loaded.add(new EquipmentView(c[0], c[1], c[2], c[3], Integer.parseInt(c[4]), c[5],
                            Long.parseLong(c[6]), Long.parseLong(c[7]), Long.parseLong(c[8]), c[9], VERSION));
                }
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load playable equipment catalog", e);
        }
    }

    public record EquipmentView(String id, String nameKey, String slot, String rarity, int maxEnhanceLevel,
                                String setId, long attack, long hp, long defense, String status, String profileVersion) {}
}
