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
public class VariantCatalogService {
    private static final List<String> RESOURCES = List.of(
            "/game-data/reference/variant-census.csv",
            "/game-data/reference/variant-census-expanded.csv");
    private final List<HeroVariantEntry> entries;

    public VariantCatalogService() {
        List<HeroVariantEntry> loaded = new ArrayList<>();
        for (String resource : RESOURCES) load(resource, loaded);
        entries = List.copyOf(loaded);
    }

    private static void load(String resource, List<HeroVariantEntry> target) {
        try (InputStream input = VariantCatalogService.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("missing packaged variant census: " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length < 3) throw new IllegalStateException("invalid variant row: " + line);
                    target.add(new HeroVariantEntry(cells[0].trim(), cells[1].trim(), cells[2].trim()));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot load variants: " + resource, e);
        }
    }

    public List<HeroVariantEntry> all() { return entries; }
    public List<HeroVariantEntry> forCharacter(String characterId) {
        return entries.stream().filter(it -> it.characterId().equals(characterId)).toList();
    }
}
