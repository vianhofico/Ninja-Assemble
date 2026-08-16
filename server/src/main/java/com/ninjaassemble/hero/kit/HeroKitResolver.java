package com.ninjaassemble.hero.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pure domain resolver for the post-M43 model. A Hero Version owns exactly five base slots and optionally one
 * Awakening Skill. There is intentionally no character/variant/generic fallback in this class.
 */
public final class HeroKitResolver {
    private final Map<String, HeroKitDefinition> heroKits;
    private final Map<String, String> awakeningSkills;

    public HeroKitResolver(Map<String, HeroKitDefinition> heroKits, Map<String, String> awakeningSkills) {
        this.heroKits = Map.copyOf(heroKits);
        this.awakeningSkills = Map.copyOf(awakeningSkills);
        for (Map.Entry<String, HeroKitDefinition> entry : this.heroKits.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) throw new IllegalArgumentException("hero id required");
            if (entry.getValue() == null) throw new IllegalArgumentException("Hero Version kit required: " + entry.getKey());
            if (entry.getValue().techniques().size() != 5) throw new IllegalArgumentException("Hero Version must have exactly five base skills: " + entry.getKey());
        }
        this.awakeningSkills.forEach((heroId, skillId) -> {
            if (!this.heroKits.containsKey(heroId)) throw new IllegalArgumentException("Awakening Skill has no Hero Version: " + heroId);
            if (skillId == null || skillId.isBlank()) throw new IllegalArgumentException("Awakening Skill id required: " + heroId);
        });
    }

    public ResolvedHeroKit resolve(String heroId, boolean awakened) {
        if (heroId == null || heroId.isBlank()) throw new IllegalArgumentException("hero id required");
        HeroKitDefinition base = heroKits.get(heroId);
        if (base == null) throw new IllegalArgumentException("no explicit kit for Hero Version: " + heroId);
        String awakeningSkill = null;
        if (awakened) {
            awakeningSkill = awakeningSkills.get(heroId);
            if (awakeningSkill == null) throw new IllegalStateException("Hero Version is marked awakened but has no Awakening Skill: " + heroId);
        }
        return new ResolvedHeroKit(heroId, awakened, base, awakeningSkill);
    }

    public record ResolvedHeroKit(String heroId, boolean awakened, HeroKitDefinition base, String awakeningSkill) {
        public ResolvedHeroKit {
            if (heroId == null || heroId.isBlank() || base == null) throw new IllegalArgumentException("invalid resolved Hero Version kit");
            if (base.techniques().size() != 5) throw new IllegalArgumentException("base kit must contain exactly five skills");
            if (awakened && (awakeningSkill == null || awakeningSkill.isBlank())) throw new IllegalArgumentException("awakened kit requires sixth skill");
            if (!awakened && awakeningSkill != null) throw new IllegalArgumentException("normal kit cannot expose Awakening Skill");
        }

        public List<String> skills() {
            List<String> out = new ArrayList<>(awakened ? 6 : 5);
            out.addAll(base.techniques());
            if (awakened) out.add(awakeningSkill);
            return List.copyOf(out);
        }
    }
}
