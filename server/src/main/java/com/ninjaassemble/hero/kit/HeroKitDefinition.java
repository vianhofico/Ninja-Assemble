package com.ninjaassemble.hero.kit;

import java.util.List;

public record HeroKitDefinition(String profileId, String basic, String skill1, String skill2, String ultimate, String passive) {
    public HeroKitDefinition {
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profile id required");
        requireTechnique("basic", basic);
        requireTechnique("skill1", skill1);
        requireTechnique("skill2", skill2);
        requireTechnique("ultimate", ultimate);
        requireTechnique("passive", passive);
    }

    private static void requireTechnique(String slot, String techniqueId) {
        if (techniqueId == null || techniqueId.isBlank()) throw new IllegalArgumentException("kit technique id required for " + slot);
    }

    public List<String> techniques() { return List.of(basic, skill1, skill2, ultimate, passive); }
}
