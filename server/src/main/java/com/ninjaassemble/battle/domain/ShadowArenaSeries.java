package com.ninjaassemble.battle.domain;

import java.util.List;
import java.util.Objects;

public final class ShadowArenaSeries {
    public enum RoundWinner { PLAYER, OPPONENT }
    public enum SeriesWinner { PLAYER, OPPONENT, UNDECIDED }

    public SeriesWinner resolve(List<RoundWinner> completedRounds) {
        Objects.requireNonNull(completedRounds, "completedRounds");
        if (completedRounds.size() > BattleRules.SHADOW_SQUAD_COUNT) throw new IllegalArgumentException("Shadow Arena has at most 3 squad battles");
        int playerWins = 0, opponentWins = 0;
        for (RoundWinner round : completedRounds) {
            Objects.requireNonNull(round, "round winner");
            if (round == RoundWinner.PLAYER) playerWins++; else opponentWins++;
            if (playerWins == BattleRules.SHADOW_WINS_REQUIRED) return SeriesWinner.PLAYER;
            if (opponentWins == BattleRules.SHADOW_WINS_REQUIRED) return SeriesWinner.OPPONENT;
        }
        return SeriesWinner.UNDECIDED;
    }
}
