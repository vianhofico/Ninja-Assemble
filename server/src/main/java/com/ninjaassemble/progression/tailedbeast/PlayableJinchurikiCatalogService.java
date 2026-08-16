package com.ninjaassemble.progression.tailedbeast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PlayableJinchurikiCatalogService {
    private static final String RESOURCE = "/game-data/progression/jinchuriki.csv";
    private final List<HostView> hosts;

    public PlayableJinchurikiCatalogService() { hosts = load(); }

    public List<HostView> all() { return hosts; }
    public HostView require(String characterId) {
        return hosts.stream().filter(host -> host.characterId().equals(characterId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("character is not a Jinchuriki in the playable catalog: " + characterId));
    }

    private static List<HostView> load() {
        try (InputStream input = PlayableJinchurikiCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing Jinchuriki catalog: " + RESOURCE);
            List<HostView> loaded = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line; boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] c = line.split(",", -1);
                    if (c.length != 4) throw new IllegalStateException("invalid Jinchuriki row: " + line);
                    loaded.add(new HostView(c[0], c[1], c[2], c[3]));
                }
            }
            return List.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("cannot load Jinchuriki catalog", e);
        }
    }

    public record HostView(String characterId, String beast, String trackId, String status) {}
}
