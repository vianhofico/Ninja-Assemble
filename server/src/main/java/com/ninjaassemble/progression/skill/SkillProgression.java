package com.ninjaassemble.progression.skill;

import java.util.HashMap;
import java.util.Map;

public final class SkillProgression {
    private final Map<String, Integer> levels = new HashMap<>();

    public int level(String skillId) { return levels.getOrDefault(skillId, 1); }

    public int upgrade(String skillId, int maxLevel) {
        if (skillId == null || skillId.isBlank() || maxLevel < 1) throw new IllegalArgumentException("invalid skill progression request");
        int current = level(skillId);
        if (current >= maxLevel) throw new IllegalStateException("skill already at max level");
        int next = current + 1;
        levels.put(skillId, next);
        return next;
    }

    public Map<String, Integer> snapshot() { return Map.copyOf(levels); }
}
