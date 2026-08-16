package com.ninjaassemble.progression.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class FrameProgressionRules {
    public record Transition(FrameTier from, FrameTier to, OptionalInt advancesRequired) {}
    private static final Map<FrameTier, Transition> TRANSITIONS = new EnumMap<>(FrameTier.class);
    static {
        TRANSITIONS.put(FrameTier.GENIN, new Transition(FrameTier.GENIN, FrameTier.CHUNIN, OptionalInt.of(1)));
        TRANSITIONS.put(FrameTier.CHUNIN, new Transition(FrameTier.CHUNIN, FrameTier.JONIN, OptionalInt.of(2)));
        TRANSITIONS.put(FrameTier.JONIN, new Transition(FrameTier.JONIN, FrameTier.KAGE, OptionalInt.of(3)));
        TRANSITIONS.put(FrameTier.KAGE, new Transition(FrameTier.KAGE, FrameTier.SIX_PATH, OptionalInt.of(4)));
        TRANSITIONS.put(FrameTier.SIX_PATH, new Transition(FrameTier.SIX_PATH, FrameTier.AWAKENING, OptionalInt.empty()));
    }
    private FrameProgressionRules() {}
    public static Optional<Transition> next(FrameTier current) { return Optional.ofNullable(TRANSITIONS.get(current)); }
}
