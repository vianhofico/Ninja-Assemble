package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.domain.DamageChannel;
import com.ninjaassemble.hero.catalog.HeroContentCatalogService;
import com.ninjaassemble.hero.domain.EffectType;
import com.ninjaassemble.hero.domain.SkillEffectDefinition;
import com.ninjaassemble.hero.domain.TargetSelector;
import com.ninjaassemble.reference.ReferenceProfiles;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class TechniqueEffectResolver {
    public static final String VERSION = ReferenceProfiles.TECHNIQUE_MAPPING;
    private static final String RESOURCE = "/game-data/skills/technique-effects.csv";
    private final Map<String, List<SkillEffectDefinition>> overrides;

    public TechniqueEffectResolver() {
        overrides = loadOverrides();
    }

    public Resolution resolve(HeroContentCatalogService.TechniqueView technique) {
        if (technique == null) throw new IllegalArgumentException("technique is required");
        List<SkillEffectDefinition> curated = overrides.get(technique.id());
        if (curated != null && !curated.isEmpty()) return new Resolution(technique.id(), MappingStatus.RUNTIME, "CURATED_ID", curated);

        String kind = technique.kind().toUpperCase(Locale.ROOT);
        if ("PASSIVE".equals(kind)) return new Resolution(technique.id(), MappingStatus.DEFERRED_PASSIVE, "PASSIVE_LIFECYCLE_PENDING", List.of());

        DamageChannel channel = DamageChannel.valueOf(technique.channel());
        Set<String> tags = Set.of(technique.tags().isBlank() ? new String[0] : technique.tags().toLowerCase(Locale.ROOT).split("\\|"));
        List<SkillEffectDefinition> effects = new ArrayList<>();

        if ("BASIC".equals(kind)) {
            effects.add(damage(TargetSelector.FRONTMOST_ENEMY, channel, 10_000));
            return new Resolution(technique.id(), MappingStatus.RUNTIME, "KIND_BASIC", effects);
        }

        if ("ULTIMATE".equals(kind)) {
            if (tags.contains("heal")) {
                effects.add(new SkillEffectDefinition(EffectType.HEAL, TargetSelector.ALL_ALLIES, channel, 18_000, 0, null, 10_000, 0));
                effects.add(new SkillEffectDefinition(EffectType.CLEANSE, TargetSelector.ALL_ALLIES, null, 0, 0, null, 10_000, 0));
                return new Resolution(technique.id(), MappingStatus.RUNTIME, "TAG_HEAL_ULTIMATE", effects);
            }
            effects.add(damage(TargetSelector.ALL_ENEMIES, channel, 18_000));
            return new Resolution(technique.id(), MappingStatus.RUNTIME, tags.contains("burst") ? "TAG_BURST_ULTIMATE" : "KIND_ULTIMATE", effects);
        }

        if (tags.contains("poison")) {
            effects.add(damage(TargetSelector.ALL_ENEMIES, channel, 6_000));
            effects.add(new SkillEffectDefinition(EffectType.STATUS, TargetSelector.ALL_ENEMIES, channel, 2_000, 0, "POISON", 10_000, 3));
            return new Resolution(technique.id(), MappingStatus.RUNTIME, "TAG_POISON", effects);
        }
        if (tags.contains("genjutsu") || tags.contains("mind")) {
            effects.add(damage(TargetSelector.FRONTMOST_ENEMY, channel, 6_000));
            effects.add(new SkillEffectDefinition(EffectType.STATUS, TargetSelector.FRONTMOST_ENEMY, channel, 0, 0, "STUN", 10_000, 1));
            return new Resolution(technique.id(), MappingStatus.RUNTIME, tags.contains("genjutsu") ? "TAG_GENJUTSU" : "TAG_MIND", effects);
        }
        if (tags.contains("kamui")) {
            effects.add(damage(TargetSelector.LOWEST_HP_ENEMY, channel, 15_000));
            return new Resolution(technique.id(), MappingStatus.RUNTIME, "TAG_KAMUI", effects);
        }

        effects.add(damage(TargetSelector.FRONTMOST_ENEMY, channel, 12_500));
        return new Resolution(technique.id(), MappingStatus.RUNTIME, "ACTIVE_DEFAULT", effects);
    }

    public int curatedTechniqueCount() { return overrides.size(); }

    private static SkillEffectDefinition damage(TargetSelector target, DamageChannel channel, int coefficientBps) {
        return new SkillEffectDefinition(EffectType.DAMAGE, target, channel, coefficientBps, 0, null, 10_000, 0);
    }

    private static Map<String, List<SkillEffectDefinition>> loadOverrides() {
        try (InputStream input = TechniqueEffectResolver.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("missing technique effect overrides: " + RESOURCE);
            Map<String, List<IndexedEffect>> grouped = new LinkedHashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                boolean header = true;
                while ((line = reader.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (line.isBlank()) continue;
                    String[] cells = line.split(",", -1);
                    if (cells.length != 14) throw new IllegalStateException("invalid technique effect override row: " + line);
                    if (!VERSION.equals(cells[10]) || !"EXPERIMENTAL_RUNTIME".equals(cells[11])) continue;
                    SkillEffectDefinition effect = new SkillEffectDefinition(
                            EffectType.valueOf(cells[2]),
                            TargetSelector.valueOf(cells[3]),
                            cells[4].isBlank() ? null : DamageChannel.valueOf(cells[4]),
                            integer(cells[5]),
                            longValue(cells[6]),
                            cells[7].isBlank() ? null : cells[7],
                            integer(cells[8]),
                            integer(cells[9]));
                    grouped.computeIfAbsent(cells[0], ignored -> new ArrayList<>()).add(new IndexedEffect(integer(cells[1]), effect));
                }
            }
            Map<String, List<SkillEffectDefinition>> result = new HashMap<>();
            grouped.forEach((id, values) -> result.put(id, values.stream()
                    .sorted(Comparator.comparingInt(IndexedEffect::index))
                    .map(IndexedEffect::effect)
                    .toList()));
            return Map.copyOf(result);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read technique effect overrides", e);
        }
    }

    private static int integer(String value) { return value == null || value.isBlank() ? 0 : Integer.parseInt(value); }
    private static long longValue(String value) { return value == null || value.isBlank() ? 0 : Long.parseLong(value); }

    public enum MappingStatus { RUNTIME, DEFERRED_PASSIVE }
    public record Resolution(String techniqueId, MappingStatus status, String basis, List<SkillEffectDefinition> effects) {
        public Resolution { effects = effects == null ? List.of() : List.copyOf(effects); }
    }
    private record IndexedEffect(int index, SkillEffectDefinition effect) {}
}
