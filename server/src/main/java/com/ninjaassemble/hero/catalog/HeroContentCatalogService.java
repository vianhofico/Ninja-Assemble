package com.ninjaassemble.hero.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class HeroContentCatalogService {
    private static final List<String> TECHNIQUE_RESOURCES = List.of(
            "/game-data/skills/technique-library-01.csv",
            "/game-data/skills/technique-library-02.csv",
            "/game-data/skills/technique-library-03.csv",
            "/game-data/skills/technique-library-04.csv");

    private final Map<String, TechniqueView> techniques = new LinkedHashMap<>();
    private final Map<String, KitIds> profiles = new LinkedHashMap<>();
    private final Map<String, String> characterProfiles = new HashMap<>();
    private final Map<String, String> variantProfiles = new HashMap<>();

    public HeroContentCatalogService() {
        for (String resource : TECHNIQUE_RESOURCES) loadTechniques(resource);
        loadProfiles();
        loadCharacterMap();
        loadVariantOverrides();
    }

    public HeroKitView resolve(String characterId, String variant) {
        String profileId = variant == null || variant.isBlank() ? null : variantProfiles.get(variantKey(characterId, variant));
        if (profileId == null) profileId = characterProfiles.get(characterId);
        if (profileId == null) throw new IllegalArgumentException("no kit profile for character: " + characterId);
        KitIds kit = profiles.get(profileId);
        if (kit == null) throw new IllegalStateException("missing profile: " + profileId);
        return new HeroKitView(profileId, List.of(
                requireTechnique(kit.basic()), requireTechnique(kit.skill1()), requireTechnique(kit.skill2()),
                requireTechnique(kit.ultimate()), requireTechnique(kit.passive())));
    }

    public List<TechniqueView> allTechniques() { return List.copyOf(techniques.values()); }
    public TechniqueView technique(String id) { return requireTechnique(id); }
    public int techniqueCount() { return techniques.size(); }
    public int profileCount() { return profiles.size(); }
    public int mappedCharacterCount() { return characterProfiles.size(); }

    private TechniqueView requireTechnique(String id) {
        TechniqueView value = techniques.get(id);
        if (value == null) throw new IllegalStateException("missing technique: " + id);
        return value;
    }

    private void loadTechniques(String resource) {
        forEachDataRow(resource, cells -> {
            if (cells.length < 9) throw new IllegalStateException("invalid technique row in " + resource);
            TechniqueView view = new TechniqueView(cells[0], cells[1], cells[2], cells[3], cells[4], cells[5], cells[6], cells[7]);
            if (techniques.putIfAbsent(view.id(), view) != null) throw new IllegalStateException("duplicate technique: " + view.id());
        });
    }

    private void loadProfiles() {
        forEachDataRow("/game-data/skills/kit-profiles.csv", cells -> {
            if (cells.length < 6) throw new IllegalStateException("invalid kit profile row");
            KitIds kit = new KitIds(cells[0], cells[1], cells[2], cells[3], cells[4], cells[5]);
            if (profiles.putIfAbsent(kit.profileId(), kit) != null) throw new IllegalStateException("duplicate profile: " + kit.profileId());
        });
    }

    private void loadCharacterMap() {
        forEachDataRow("/game-data/skills/character-kit-map.csv", cells -> {
            if (cells.length < 2) throw new IllegalStateException("invalid character kit mapping");
            characterProfiles.put(cells[0], cells[1]);
        });
    }

    private void loadVariantOverrides() {
        forEachDataRow("/game-data/skills/variant-kit-overrides.csv", cells -> {
            if (cells.length < 3) throw new IllegalStateException("invalid variant kit override");
            variantProfiles.put(variantKey(cells[0], cells[1]), cells[2]);
        });
    }

    private static String variantKey(String characterId, String variant) { return characterId + "\u0000" + variant; }

    private static void forEachDataRow(String resource, RowConsumer consumer) {
        try (InputStream input = HeroContentCatalogService.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("missing packaged game-data resource: " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    consumer.accept(line.split(",", -1));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read packaged game-data resource: " + resource, e);
        }
    }

    @FunctionalInterface private interface RowConsumer { void accept(String[] cells); }

    private record KitIds(String profileId, String basic, String skill1, String skill2, String ultimate, String passive) {}

    public record TechniqueView(String id, String nameEn, String nameVi, String descriptionEn, String descriptionVi,
                                String channel, String kind, String tags) {}
    public record HeroKitView(String profileId, List<TechniqueView> techniques) {}
}
