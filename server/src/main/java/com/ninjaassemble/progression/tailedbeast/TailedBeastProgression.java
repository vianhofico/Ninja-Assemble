package com.ninjaassemble.progression.tailedbeast;

import com.ninjaassemble.progression.domain.FrameTier;

public record TailedBeastProgression(String trackId, int stage) {
    public TailedBeastProgression {
        if (trackId == null || trackId.isBlank() || stage < 0) throw new IllegalArgumentException("invalid tailed-beast state");
    }

    public AdvanceResult canAdvance(TailedBeastTrackDefinition track, FrameTier frameTier, long souls, long beastBones) {
        if (!track.id().equals(trackId)) return new AdvanceResult(false, "wrong-track");
        if (stage >= track.stages().size()) return new AdvanceResult(false, "max-stage");
        TailedBeastTrackDefinition.Stage next = track.stages().get(stage);
        if (frameTier.ordinal() < next.requiredFrameTier().ordinal()) return new AdvanceResult(false, "frame-tier");
        if (souls < next.soulCost()) return new AdvanceResult(false, "souls");
        if (beastBones < next.beastBoneCost()) return new AdvanceResult(false, "beast-bones");
        return new AdvanceResult(true, null);
    }

    public TailedBeastProgression advance(TailedBeastTrackDefinition track, FrameTier frameTier, long souls, long beastBones) {
        AdvanceResult gate = canAdvance(track, frameTier, souls, beastBones);
        if (!gate.allowed()) throw new IllegalStateException("cannot advance tailed-beast track: " + gate.reason());
        return new TailedBeastProgression(trackId, stage + 1);
    }

    public record AdvanceResult(boolean allowed, String reason) {}
}
