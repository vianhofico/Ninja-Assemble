package com.ninjaassemble.progression.tailedbeast;

import com.ninjaassemble.progression.domain.FrameTier;
import java.util.Comparator;
import java.util.List;

public record TailedBeastTrackDefinition(String id, List<Stage> stages) {
    public TailedBeastTrackDefinition {
        if (id == null || id.isBlank() || stages == null || stages.isEmpty()) throw new IllegalArgumentException("track and stages required");
        stages = stages.stream().sorted(Comparator.comparingInt(Stage::index)).toList();
        for (int i = 0; i < stages.size(); i++) if (stages.get(i).index() != i + 1) throw new IllegalArgumentException("tailed-beast stages must be sequential from 1");
    }

    public record Stage(int index, String nameKey, long soulCost, long beastBoneCost, FrameTier requiredFrameTier) {
        public Stage {
            if (index < 1 || nameKey == null || nameKey.isBlank() || soulCost < 0 || beastBoneCost < 0 || requiredFrameTier == null) throw new IllegalArgumentException("invalid stage");
        }
    }
}
