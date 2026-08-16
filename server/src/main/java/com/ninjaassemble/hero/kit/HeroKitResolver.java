package com.ninjaassemble.hero.kit;

import java.util.Map;
import java.util.Objects;

public final class HeroKitResolver {
    private final Map<String, HeroKitDefinition> profiles;
    private final Map<String, String> characterProfiles;
    private final Map<VariantKey, String> variantOverrides;

    public HeroKitResolver(Map<String, HeroKitDefinition> profiles, Map<String, String> characterProfiles, Map<VariantKey, String> variantOverrides) {
        this.profiles = Map.copyOf(profiles);
        this.characterProfiles = Map.copyOf(characterProfiles);
        this.variantOverrides = Map.copyOf(variantOverrides);
        characterProfiles.forEach((character, profile) -> requireProfile(profile));
        variantOverrides.forEach((key, profile) -> requireProfile(profile));
    }

    public HeroKitDefinition resolve(String characterId, String variant) {
        if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("character id required");
        String profileId = variant == null ? null : variantOverrides.get(new VariantKey(characterId, variant));
        if (profileId == null) profileId = characterProfiles.get(characterId);
        if (profileId == null) throw new IllegalArgumentException("no kit mapping for character: " + characterId);
        return requireProfile(profileId);
    }

    private HeroKitDefinition requireProfile(String profileId) {
        HeroKitDefinition profile = profiles.get(profileId);
        if (profile == null) throw new IllegalArgumentException("unknown kit profile: " + profileId);
        return profile;
    }

    public record VariantKey(String characterId, String variant) {
        public VariantKey { Objects.requireNonNull(characterId); Objects.requireNonNull(variant); }
    }
}
