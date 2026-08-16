package com.ninjaassemble.hero.catalog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class HeroContentCatalogService {
    private static final List<String> TECHNIQUE_RESOURCES = List.of(
            "/game-data/skills/technique-library-01.csv",
            "/game-data/skills/technique-library-02.csv",
            "/game-data/skills/technique-library-03.csv",
            "/game-data/skills/technique-library-04.csv");
    private static final List<String> BASE_SLOTS = List.of("BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE");

    private final Map<String, TechniqueView> techniques = new LinkedHashMap<>();
    private final Map<String, HeroVersionRow> heroes = new LinkedHashMap<>();
    private final Map<String, HeroSkillAlias> heroSkills = new LinkedHashMap<>();
    private final Map<String, AwakeningSkillRow> awakeningSkillsByHero = new LinkedHashMap<>();
    private final Map<String, LegacyIdentity> legacyIdentities = new HashMap<>();
    private final Set<String> mappedCharacters = new LinkedHashSet<>();

    public HeroContentCatalogService() {
        for (String resource : TECHNIQUE_RESOURCES) loadTechniques(resource);
        loadHeroSkills();
        loadHeroSkillOverrides();
        loadHeroes();
        loadAwakeningSkills();
        loadLegacyBridge();
        validateCatalog();
    }

    /**
     * Production runtime resolver. No character profile, generic profile or variant fallback is allowed here.
     */
    public HeroKitView resolveHero(String heroId, boolean awakened) {
        if (heroId == null || heroId.isBlank()) throw new IllegalArgumentException("hero id required");
        HeroVersionRow hero = heroes.get(heroId);
        if (hero == null) throw new IllegalArgumentException("no explicit Hero Version kit: " + heroId);

        List<HeroSkillView> skills = new ArrayList<>(awakened ? 6 : 5);
        List<TechniqueView> baseTechniques = new ArrayList<>(5);
        for (String slot : BASE_SLOTS) {
            String skillId = hero.skillId(slot);
            HeroSkillAlias alias = heroSkills.get(skillId);
            if (alias == null) throw new IllegalStateException(heroId + " missing explicit skill alias for " + slot + ": " + skillId);
            if (!heroId.equals(alias.heroId()) || !slot.equals(alias.slot())) {
                throw new IllegalStateException(heroId + " skill alias ownership/slot mismatch: " + skillId);
            }
            TechniqueView technique = requireTechnique(alias.sourceTechniqueId());
            baseTechniques.add(technique);
            skills.add(new HeroSkillView(
                    alias.skillId(), alias.slot(), alias.sourceTechniqueId(), technique.nameEn(), technique.nameVi(),
                    technique.descriptionEn(), technique.descriptionVi(), technique.channel(), technique.kind(), technique.tags(),
                    true, alias.status()));
        }

        if (awakened) {
            if (hero.awakeningId() == null || hero.awakeningId().isBlank()) {
                throw new IllegalStateException("owned hero is marked awakened but Hero Version has no Awakening: " + heroId);
            }
            AwakeningSkillRow awakening = awakeningSkillsByHero.get(heroId);
            if (awakening == null || !hero.awakeningId().equals(awakening.awakeningId())) {
                throw new IllegalStateException("missing unique Awakening Skill for " + heroId);
            }
            boolean executable = "RUNTIME".equals(awakening.status()) || "PLAYABLE_RUNTIME".equals(awakening.status());
            skills.add(new HeroSkillView(
                    awakening.skillId(), "AWAKENING_SKILL", "", awakening.nameEn(), awakening.nameVi(),
                    awakening.descriptionEn(), awakening.descriptionVi(), awakening.element(), "AWAKENING_SKILL", "awakening",
                    executable, awakening.status()));
        }

        int expected = awakened ? 6 : 5;
        if (skills.size() != expected) throw new IllegalStateException(heroId + " expected " + expected + " runtime slots, got " + skills.size());
        return new HeroKitView("hero-version:" + heroId, heroId, awakened, List.copyOf(baseTechniques), List.copyOf(skills));
    }

    /**
     * Compatibility resolver for legacy enemy/stage data only. It translates through the audited 427-row bridge,
     * then delegates to resolveHero. It never falls back to character-kit-map or generic profiles.
     */
    @Deprecated(forRemoval = true)
    public HeroKitView resolve(String characterId, String variant) {
        String normalized = variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE") ? "__BASE__" : variant;
        LegacyIdentity identity = legacyIdentities.get(legacyKey(characterId, normalized));
        if (identity == null) throw new IllegalArgumentException("no audited legacy Hero Version mapping: " + characterId + " / " + normalized);
        if (identity.heroId() == null || identity.heroId().isBlank()) {
            throw new IllegalArgumentException("legacy content is not a normal Hero Version: " + characterId + " / " + normalized);
        }
        return resolveHero(identity.heroId(), identity.awakened());
    }

    public HeroIdentity resolveLegacyIdentity(String characterId, String variant) {
        String normalized = variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE") ? "__BASE__" : variant;
        LegacyIdentity identity = legacyIdentities.get(legacyKey(characterId, normalized));
        if (identity == null || identity.heroId() == null || identity.heroId().isBlank()) {
            throw new IllegalArgumentException("legacy content has no collectible Hero Version identity: " + characterId + " / " + normalized);
        }
        return new HeroIdentity(identity.heroId(), identity.awakened());
    }

    public List<TechniqueView> allTechniques() { return List.copyOf(techniques.values()); }
    public TechniqueView technique(String id) { return requireTechnique(id); }
    public int techniqueCount() { return techniques.size(); }
    public int profileCount() { return heroes.size(); }
    public int heroCount() { return heroes.size(); }
    public int heroSkillCount() { return heroSkills.size(); }
    public int awakeningSkillCount() { return awakeningSkillsByHero.size(); }
    public int mappedCharacterCount() { return mappedCharacters.size(); }

    private TechniqueView requireTechnique(String id) {
        TechniqueView value = techniques.get(id);
        if (value == null) throw new IllegalStateException("missing technique: " + id);
        return value;
    }

    private void loadTechniques(String resource) {
        forEachDataRow(resource, cells -> {
            if (cells.size() < 9) throw new IllegalStateException("invalid technique row in " + resource);
            TechniqueView view = new TechniqueView(cells.get(0), cells.get(1), cells.get(2), cells.get(3), cells.get(4), cells.get(5), cells.get(6), cells.get(7));
            if (techniques.putIfAbsent(view.id(), view) != null) throw new IllegalStateException("duplicate technique: " + view.id());
        });
    }

    private void loadHeroSkills() {
        forEachDataRow("/game-data/skills/hero-version-skills.csv", cells -> {
            if (cells.size() < 12) throw new IllegalStateException("invalid Hero Version skill alias row");
            HeroSkillAlias row = new HeroSkillAlias(cells.get(0), cells.get(1), cells.get(2), cells.get(3), cells.get(10));
            if (heroSkills.putIfAbsent(row.skillId(), row) != null) throw new IllegalStateException("duplicate Hero Version skill id: " + row.skillId());
        });
    }

    /**
     * M47 keeps the generated 970-row alias catalog reproducible, then overlays a small reviewed set of
     * version-identity corrections. These rows are deliberately baseline/research statuses; M50 owns final
     * canon evidence, Rage/timing and cinematic design.
     */
    private void loadHeroSkillOverrides() {
        forEachDataRow("/game-data/skills/hero-version-skill-overrides.csv", cells -> {
            if (cells.size() < 6) throw new IllegalStateException("invalid Hero Version skill override row");
            String skillId = cells.get(0);
            String sourceTechniqueId = cells.get(1);
            String status = cells.get(4);
            HeroSkillAlias current = heroSkills.get(skillId);
            if (current == null) throw new IllegalStateException("Hero Version override references unknown skill: " + skillId);
            requireTechnique(sourceTechniqueId);
            heroSkills.put(skillId, new HeroSkillAlias(
                    current.skillId(), current.heroId(), current.slot(), sourceTechniqueId, status));
        });
    }

    private void loadHeroes() {
        forEachDataRow("/game-data/heroes/heroes.csv", cells -> {
            if (cells.size() < 19) throw new IllegalStateException("invalid Hero Version row");
            HeroVersionRow row = new HeroVersionRow(
                    cells.get(0), cells.get(1), cells.get(3), cells.get(11), cells.get(12), cells.get(13), cells.get(14), cells.get(15), cells.get(16));
            if (heroes.putIfAbsent(row.heroId(), row) != null) throw new IllegalStateException("duplicate Hero Version: " + row.heroId());
            mappedCharacters.add(row.characterId());
        });
    }

    private void loadAwakeningSkills() {
        forEachDataRow("/game-data/skills/awakening-skills.csv", cells -> {
            if (cells.size() < 25) throw new IllegalStateException("invalid Awakening Skill row");
            AwakeningSkillRow row = new AwakeningSkillRow(
                    cells.get(0), cells.get(1), cells.get(2), cells.get(3), cells.get(4), cells.get(6),
                    cells.get(20), cells.get(21), cells.get(24));
            if (awakeningSkillsByHero.putIfAbsent(row.heroId(), row) != null) {
                throw new IllegalStateException("more than one Awakening Skill for Hero Version: " + row.heroId());
            }
        });
    }

    private void loadLegacyBridge() {
        forEachDataRow("/game-data/migration/legacy-hero-version-map.csv", cells -> {
            if (cells.size() < 6) throw new IllegalStateException("invalid legacy Hero Version bridge row");
            String heroId = cells.get(2).isBlank() ? null : cells.get(2);
            LegacyIdentity row = new LegacyIdentity(heroId, Boolean.parseBoolean(cells.get(3)), cells.get(4));
            String key = legacyKey(cells.get(0), cells.get(1));
            if (legacyIdentities.putIfAbsent(key, row) != null) throw new IllegalStateException("duplicate legacy bridge key: " + key);
        });
    }

    private void validateCatalog() {
        for (HeroVersionRow hero : heroes.values()) {
            for (String slot : BASE_SLOTS) {
                String skillId = hero.skillId(slot);
                HeroSkillAlias alias = heroSkills.get(skillId);
                if (alias == null) throw new IllegalStateException(hero.heroId() + " missing " + slot + " alias " + skillId);
                if (!hero.heroId().equals(alias.heroId()) || !slot.equals(alias.slot())) {
                    throw new IllegalStateException(hero.heroId() + " has non-owned/non-slot alias " + skillId);
                }
                requireTechnique(alias.sourceTechniqueId());
            }
            if (hero.awakeningId() != null && !hero.awakeningId().isBlank()) {
                AwakeningSkillRow awakening = awakeningSkillsByHero.get(hero.heroId());
                if (awakening == null || !hero.awakeningId().equals(awakening.awakeningId())) {
                    throw new IllegalStateException(hero.heroId() + " has no exact one-to-one Awakening Skill");
                }
            } else if (awakeningSkillsByHero.containsKey(hero.heroId())) {
                throw new IllegalStateException(hero.heroId() + " has Awakening Skill without Awakening");
            }
        }
        if (heroSkills.size() != heroes.size() * 5) {
            throw new IllegalStateException("runtime catalog requires exactly five base aliases per Hero Version");
        }
    }

    private static String legacyKey(String characterId, String variant) { return characterId + "\u0000" + variant; }

    private static void forEachDataRow(String resource, RowConsumer consumer) {
        try (InputStream input = HeroContentCatalogService.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("missing packaged game-data resource: " + resource);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    consumer.accept(parseCsvLine(line));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read packaged game-data resource: " + resource, e);
        }
    }

    static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"'); i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                cells.add(cell.toString()); cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        if (quoted) throw new IllegalStateException("unterminated quoted CSV field");
        cells.add(cell.toString());
        return cells;
    }

    @FunctionalInterface private interface RowConsumer { void accept(List<String> cells); }

    private record HeroVersionRow(String heroId, String characterId, String displayName, String basic, String skill1,
                                  String skill2, String ultimate, String passive, String awakeningId) {
        String skillId(String slot) {
            return switch (slot) {
                case "BASIC" -> basic;
                case "SKILL_1" -> skill1;
                case "SKILL_2" -> skill2;
                case "ULTIMATE" -> ultimate;
                case "PASSIVE" -> passive;
                default -> throw new IllegalArgumentException("unknown skill slot: " + slot);
            };
        }
    }
    private record HeroSkillAlias(String skillId, String heroId, String slot, String sourceTechniqueId, String status) {}
    private record AwakeningSkillRow(String skillId, String heroId, String awakeningId, String nameEn, String nameVi,
                                     String element, String descriptionEn, String descriptionVi, String status) {}
    private record LegacyIdentity(String heroId, boolean awakened, String mappingKind) {}

    public record HeroIdentity(String heroId, boolean awakened) {}
    public record TechniqueView(String id, String nameEn, String nameVi, String descriptionEn, String descriptionVi,
                                String channel, String kind, String tags) {}
    public record HeroSkillView(String skillId, String slot, String sourceTechniqueId, String nameEn, String nameVi,
                                String descriptionEn, String descriptionVi, String channel, String kind, String tags,
                                boolean executable, String status) {}
    public record HeroKitView(String profileId, String heroId, boolean awakened,
                              List<TechniqueView> techniques, List<HeroSkillView> skills) {
        public HeroKitView {
            techniques = List.copyOf(techniques);
            skills = List.copyOf(skills);
            int expected = awakened ? 6 : 5;
            if (skills.size() != expected) throw new IllegalArgumentException("HeroKitView expected " + expected + " skills");
            if (techniques.size() != 5) throw new IllegalArgumentException("HeroKitView requires five base source techniques");
        }
        public HeroSkillView skill(String slot) {
            return skills.stream().filter(it -> it.slot().equals(slot)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("kit has no slot: " + slot));
        }
    }
}
