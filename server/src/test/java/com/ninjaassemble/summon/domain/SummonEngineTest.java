package com.ninjaassemble.summon.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummonEngineTest {
    @Test
    void sameSeedAndStateProduceSamePull() {
        SummonBannerDefinition banner = banner();
        SummonEngine engine = new SummonEngine();
        assertEquals(engine.pull(banner, new SummonState(0), 42), engine.pull(banner, new SummonState(0), 42));
    }

    @Test
    void hardPityRestrictsPoolToConfiguredRarityOrHigher() {
        SummonEngine.PullResult result = new SummonEngine().pull(banner(), new SummonState(9), 7);
        assertTrue(result.pityTriggered());
        assertTrue(result.entry().rarity().ordinal() >= SummonRarity.SSR.ordinal());
        assertEquals(0, result.nextState().pullsSincePity());
    }

    private static SummonBannerDefinition banner() {
        return new SummonBannerDefinition("test", "v1", "DIAMOND", 100, 10, SummonRarity.SSR, List.of(
                new SummonPoolEntry("naruto", SummonRarity.SR, 9000, false),
                new SummonPoolEntry("naruto-sage", SummonRarity.SSR, 900, true),
                new SummonPoolEntry("naruto-six-paths", SummonRarity.UR, 100, true)));
    }
}
