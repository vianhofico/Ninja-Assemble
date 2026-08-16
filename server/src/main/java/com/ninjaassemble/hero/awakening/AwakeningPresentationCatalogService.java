package com.ninjaassemble.hero.awakening;

import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
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
public final class AwakeningPresentationCatalogService {
    private static final String RESOURCE = "/game-data/assets/awakening-visuals.csv";
    private final Map<String, AwakeningPresentation> byAwakeningId = new LinkedHashMap<>();

    public AwakeningPresentationCatalogService() {
        load();
    }

    public AwakeningPresentation require(String awakeningId) {
        AwakeningPresentation value = byAwakeningId.get(awakeningId);
        if (value == null) throw new IllegalArgumentException("unknown Awakening presentation: " + awakeningId);
        return value;
    }

    public AwakeningPresentation findForHero(String heroId) {
        return byAwakeningId.values().stream().filter(value -> value.heroId().equals(heroId)).findFirst().orElse(null);
    }

    public int size() { return byAwakeningId.size(); }

    private void load() {
        try (InputStream input = AwakeningPresentationCatalogService.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing Awakening visual catalog: " + RESOURCE);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    List<String> cells = HeroContentCatalogService.parseCsvLine(line);
                    if (cells.size() < 27) throw new IllegalStateException("invalid Awakening visual row");
                    AwakeningPresentation value = new AwakeningPresentation(
                            cells.get(0), cells.get(1),
                            cells.get(12), cells.get(13), cells.get(14),
                            cells.get(15), cells.get(16),
                            cells.get(17), cells.get(18), cells.get(19), cells.get(20), cells.get(21),
                            cells.get(22), cells.get(23), cells.get(24), cells.get(25), cells.get(26));
                    if (value.awakeningId().isBlank() || value.heroId().isBlank()) {
                        throw new IllegalStateException("invalid Awakening presentation identity");
                    }
                    if (byAwakeningId.putIfAbsent(value.awakeningId(), value) != null) {
                        throw new IllegalStateException("duplicate Awakening presentation: " + value.awakeningId());
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read Awakening visual catalog", e);
        }
    }

    public record AwakeningPresentation(
            String awakeningId,
            String heroId,
            String transitionStart,
            String transitionMid,
            String transitionEnd,
            String idleAnimation,
            String movementAnimation,
            String basicVfxModifier,
            String skill1VfxModifier,
            String skill2VfxModifier,
            String ultimateVfxModifier,
            String awakeningSkillVfx,
            String cameraSequence,
            String screenEffect,
            String sfxDescription,
            String referenceSource,
            String status
    ) {}
}
