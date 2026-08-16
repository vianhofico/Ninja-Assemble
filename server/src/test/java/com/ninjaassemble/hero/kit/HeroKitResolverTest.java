package com.ninjaassemble.hero.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeroKitResolverTest {
    @Test
    void variantOverrideWinsOverBaseCharacterProfile() {
        HeroKitDefinition base = new HeroKitDefinition("naruto", "a", "b", "c", "d", "e");
        HeroKitDefinition sage = new HeroKitDefinition("naruto-sage", "a", "f", "b", "d", "g");
        HeroKitResolver resolver = new HeroKitResolver(
                Map.of("naruto", base, "naruto-sage", sage),
                Map.of("naruto-uzumaki", "naruto"),
                Map.of(new HeroKitResolver.VariantKey("naruto-uzumaki", "Sage Mode"), "naruto-sage"));
        assertEquals("naruto", resolver.resolve("naruto-uzumaki", null).profileId());
        assertEquals("naruto-sage", resolver.resolve("naruto-uzumaki", "Sage Mode").profileId());
    }
}
