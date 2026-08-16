package com.ninjaassemble.campaign.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CampaignGate {
    private CampaignGate() {}

    public static GateResult evaluate(StageDefinition stage, int playerLevel, int currentEnergy, Set<String> completedStages) {
        List<String> missing = new ArrayList<>();
        if (playerLevel < stage.minPlayerLevel()) missing.add("player-level:" + stage.minPlayerLevel());
        if (currentEnergy < stage.energyCost()) missing.add("energy:" + stage.energyCost());
        Set<String> completed = completedStages == null ? Set.of() : completedStages;
        for (String prerequisite : stage.prerequisiteStageIds()) if (!completed.contains(prerequisite)) missing.add("stage:" + prerequisite);
        return new GateResult(missing.isEmpty(), List.copyOf(missing));
    }

    public record GateResult(boolean allowed, List<String> missing) {}
}
