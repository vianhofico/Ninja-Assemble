package com.ninjaassemble.campaign.domain;

import java.util.HashSet;
import java.util.List;

public record WaveDefinition(int index, List<EnemySlotDefinition> enemies) {
    public WaveDefinition {
        if (index < 1 || enemies == null || enemies.isEmpty() || enemies.size() > 5) throw new IllegalArgumentException("invalid wave");
        enemies = List.copyOf(enemies);
        var slots = new HashSet<Integer>();
        for (EnemySlotDefinition enemy : enemies) if (!slots.add(enemy.slot())) throw new IllegalArgumentException("duplicate enemy slot");
    }
}
