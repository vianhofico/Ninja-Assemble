package com.ninjaassemble.pvp.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ninjaassemble.battle.domain.ShadowArenaSeries;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShadowArenaMatchResolverTest {
    private final ShadowArenaMatchResolver resolver = new ShadowArenaMatchResolver();

    @Test
    void resolvesBestOfThreeAsSoonAsEitherSideReachesTwoWins() {
        assertEquals(ShadowArenaSeries.SeriesWinner.UNDECIDED, resolver.resolve(List.of(true)));
        assertEquals(ShadowArenaSeries.SeriesWinner.PLAYER, resolver.resolve(List.of(true, true)));
        assertEquals(ShadowArenaSeries.SeriesWinner.OPPONENT, resolver.resolve(List.of(false, false)));
        assertEquals(ShadowArenaSeries.SeriesWinner.PLAYER, resolver.resolve(List.of(false, true, true)));
        assertEquals(ShadowArenaSeries.SeriesWinner.OPPONENT, resolver.resolve(List.of(true, false, false)));
    }
}
