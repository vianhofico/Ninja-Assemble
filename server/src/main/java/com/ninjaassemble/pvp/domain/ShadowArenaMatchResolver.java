package com.ninjaassemble.pvp.domain;

import com.ninjaassemble.battle.domain.ShadowArenaSeries;
import java.util.List;

public final class ShadowArenaMatchResolver {
    private final ShadowArenaSeries series = new ShadowArenaSeries();

    public ShadowArenaSeries.SeriesWinner resolve(List<Boolean> playerWonRounds) {
        if (playerWonRounds == null) throw new IllegalArgumentException("round results required");
        return series.resolve(playerWonRounds.stream()
                .map(won -> won ? ShadowArenaSeries.RoundWinner.PLAYER : ShadowArenaSeries.RoundWinner.OPPONENT)
                .toList());
    }
}
