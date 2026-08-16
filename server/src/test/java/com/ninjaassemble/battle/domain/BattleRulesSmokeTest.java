package com.ninjaassemble.battle.domain;

import com.ninjaassemble.progression.domain.FrameProgressionRules;
import com.ninjaassemble.progression.domain.FrameTier;
import java.util.List;

public final class BattleRulesSmokeTest {
    public static void main(String[] args) {
        BattleRules.requireArenaTeamSize(5);
        BattleRules.requireShadowRosterSize(15);
        BattleRules.requireShadowSquadSize(5);
        expectIllegalArgument(() -> BattleRules.requireArenaTeamSize(4));
        expectIllegalArgument(() -> BattleRules.requireShadowRosterSize(10));
        ShadowArenaSeries series = new ShadowArenaSeries();
        assertEquals(ShadowArenaSeries.SeriesWinner.PLAYER, series.resolve(List.of(ShadowArenaSeries.RoundWinner.PLAYER, ShadowArenaSeries.RoundWinner.PLAYER)));
        assertEquals(ShadowArenaSeries.SeriesWinner.UNDECIDED, series.resolve(List.of(ShadowArenaSeries.RoundWinner.PLAYER, ShadowArenaSeries.RoundWinner.OPPONENT)));
        assertEquals(ShadowArenaSeries.SeriesWinner.OPPONENT, series.resolve(List.of(ShadowArenaSeries.RoundWinner.PLAYER, ShadowArenaSeries.RoundWinner.OPPONENT, ShadowArenaSeries.RoundWinner.OPPONENT)));
        assertAdvance(FrameTier.GENIN, FrameTier.CHUNIN, 1);
        assertAdvance(FrameTier.CHUNIN, FrameTier.JONIN, 2);
        assertAdvance(FrameTier.JONIN, FrameTier.KAGE, 3);
        assertAdvance(FrameTier.KAGE, FrameTier.SIX_PATH, 4);
        var sixPath = FrameProgressionRules.next(FrameTier.SIX_PATH).orElseThrow();
        assertEquals(FrameTier.AWAKENING, sixPath.to());
        if (sixPath.advancesRequired().isPresent()) throw new AssertionError("Six Path -> Awakening requirement must remain research-driven");
        if (FrameProgressionRules.next(FrameTier.AWAKENING).isPresent()) throw new AssertionError("Awakening must not have a guessed next tier");
        System.out.println("Verified core rules smoke test: PASS");
    }
    private static void assertAdvance(FrameTier from, FrameTier to, int advances) {
        var transition = FrameProgressionRules.next(from).orElseThrow();
        assertEquals(to, transition.to());
        if (transition.advancesRequired().orElseThrow() != advances) throw new AssertionError("Expected " + advances + " advances for " + from);
    }
    private static void expectIllegalArgument(Runnable runnable) {
        try { runnable.run(); throw new AssertionError("Expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + " but got " + actual);
    }
}
