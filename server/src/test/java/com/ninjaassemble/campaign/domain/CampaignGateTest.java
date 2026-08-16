package com.ninjaassemble.campaign.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CampaignGateTest {
    @Test
    void prerequisitesLevelAndEnergyAreEvaluatedFromStageData() {
        StageDefinition stage = new StageDefinition("1-2", 1, 2, CampaignDifficulty.NORMAL, 5, 3, Set.of("1-1"),
                List.of(new WaveDefinition(1, List.of(new EnemySlotDefinition(0, "enemy", 3, null)))),
                new RewardBundle(10, Map.of("GOLD", 20L), Map.of()), new RewardBundle(5, Map.of("GOLD", 10L), Map.of()));
        assertFalse(CampaignGate.evaluate(stage, 2, 5, Set.of("1-1")).allowed());
        assertFalse(CampaignGate.evaluate(stage, 3, 4, Set.of("1-1")).allowed());
        assertFalse(CampaignGate.evaluate(stage, 3, 5, Set.of()).allowed());
        assertTrue(CampaignGate.evaluate(stage, 3, 5, Set.of("1-1")).allowed());
    }
}
