package com.ninjaassemble.hero.kit;

import java.util.List;

public record HeroKitDefinition(String profileId, String basic, String skill1, String skill2, String ultimate, String passive) {
    public HeroKitDefinition {
        if (profileId == null || profileId.isBlank()) throw new IllegalArgumentException("profile id required");
        for (String technique : techniques()) if (technique == null || technique.isBlank()) throw new IllegalArgumentException("kit technique id required");
    }

    public List<String> techniques() { return List.of(basic, skill1, skill2, ultimate, passive); }
}
