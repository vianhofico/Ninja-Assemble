package com.ninjaassemble.hero.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Acquisition-facing projection of the approved Hero Version catalog.
 *
 * <p>This intentionally reads only the new Hero Version data model. Legacy character/variant catalogs are not
 * valid acquisition sources after M45. Awakening rows are metadata of a Hero Version and are never returned as
 * independently acquirable entries.</p>
 */
@Service
public class HeroVersionAcquisitionCatalogService {
    private static final String RESOURCE = "/game-data/heroes/heroes.csv";

    private final Map<String, HeroVersionAcquisitionEntry> entries = new LinkedHashMap<>();

    public HeroVersionAcquisitionCatalogService() {
        load();
        if (entries.isEmpty()) throw new IllegalStateException("Hero Version acquisition catalog is empty");
    }

    public List<HeroVersionAcquisitionEntry> all() {
        return List.copyOf(entries.values());
    }

    public List<HeroVersionAcquisitionEntry> summonable() {
        return entries.values().stream().filter(HeroVersionAcquisitionEntry::summonable).toList();
    }

    public HeroVersionAcquisitionEntry require(String heroId) {
        HeroVersionAcquisitionEntry entry = entries.get(heroId);
        if (entry == null) throw new IllegalArgumentException("unknown Hero Version: " + heroId);
        return entry;
    }

    private void load() {
        try (InputStream input = HeroVersionAcquisitionCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing packaged Hero Version catalog: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    List<String> cells = HeroContentCatalogService.parseCsvLine(line);
                    if (cells.size() < 19) throw new IllegalStateException("invalid Hero Version acquisition row");
                    HeroVersionAcquisitionEntry entry = new HeroVersionAcquisitionEntry(
                            cells.get(0), cells.get(1), cells.get(3), cells.get(4), cells.get(6),
                            cells.get(16), Boolean.parseBoolean(cells.get(17)), cells.get(18));
                    if (entry.heroId().isBlank() || entry.characterId().isBlank() || entry.rarity().isBlank()) {
                        throw new IllegalStateException("invalid Hero Version acquisition identity: " + line);
                    }
                    if (entries.putIfAbsent(entry.heroId(), entry) != null) {
                        throw new IllegalStateException("duplicate Hero Version acquisition id: " + entry.heroId());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read Hero Version acquisition catalog", e);
        }
    }

    public record HeroVersionAcquisitionEntry(
            String heroId,
            String characterId,
            String displayNameEn,
            String displayNameVi,
            String rarity,
            String awakeningId,
            boolean summonable,
            String status
    ) {}
}
