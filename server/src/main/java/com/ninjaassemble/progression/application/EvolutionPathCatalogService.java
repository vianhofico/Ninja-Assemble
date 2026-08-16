package com.ninjaassemble.progression.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EvolutionPathCatalogService {
    public static final String VERSION = "evolution-expansion-playable-v1";
    private static final String RESOURCE = "/game-data/progression/playable-evolution-paths.csv";
    private final List<EvolutionPath> paths;

    public EvolutionPathCatalogService() {
        paths = load();
    }

    public List<EvolutionPath> forCharacter(String characterId) {
        return paths.stream().filter(path -> path.characterId().equals(characterId)).toList();
    }

    public EvolutionPath require(String characterId, String targetVariant) {
        return paths.stream().filter(path -> path.characterId().equals(characterId) && path.targetVariant().equals(targetVariant))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("no playable evolution path: " + characterId + " -> " + targetVariant));
    }

    public List<EvolutionPath> all() { return paths; }

    private static List<EvolutionPath> load() {
        try (InputStream input = EvolutionPathCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing evolution path resource: " + RESOURCE);
            List<EvolutionPath> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line; boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length != 7) throw new IllegalStateException("invalid evolution path row: " + line);
                    loaded.add(new EvolutionPath(cells[0], cells[1], cells[2], Integer.parseInt(cells[3]), cells[4], Long.parseLong(cells[5]), cells[6], VERSION));
                }
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load evolution paths", e);
        }
    }

    public record EvolutionPath(String characterId, String targetVariant, String prerequisiteVariant, int minLevel,
                                String minFrame, long goldCost, String status, String profileVersion) {
        public EvolutionPath {
            if (characterId == null || characterId.isBlank() || targetVariant == null || targetVariant.isBlank()
                    || prerequisiteVariant == null || prerequisiteVariant.isBlank() || minLevel < 1 || goldCost < 0
                    || minFrame == null || minFrame.isBlank() || status == null || status.isBlank()) {
                throw new IllegalArgumentException("invalid evolution path");
            }
        }
    }
}
