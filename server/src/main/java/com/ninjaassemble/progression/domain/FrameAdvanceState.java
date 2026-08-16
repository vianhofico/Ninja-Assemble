package com.ninjaassemble.progression.domain;

public record FrameAdvanceState(FrameTier tier, int step) {
    public FrameAdvanceState {
        if (tier == null || step < 0) throw new IllegalArgumentException("invalid frame state");
    }

    public FrameAdvanceState advanceOne() {
        FrameProgressionRules.Transition transition = FrameProgressionRules.next(tier)
                .orElseThrow(() -> new IllegalStateException("no frame transition after " + tier));
        if (transition.advancesRequired().isEmpty()) {
            throw new IllegalStateException("transition " + tier + " -> " + transition.to() + " requires an external parity-verified gate");
        }
        int required = transition.advancesRequired().getAsInt();
        int next = step + 1;
        return next >= required ? new FrameAdvanceState(transition.to(), 0) : new FrameAdvanceState(tier, next);
    }
}
