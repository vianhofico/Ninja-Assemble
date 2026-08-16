package com.ninjaassemble.hero.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeroKitResolverTest {
    @Test
    void resolvesExactlyFiveNormalAndSixAwakenedSlotsWithoutFallback() {
        HeroKitDefinition sage = new HeroKitDefinition("naruto-sage", "a", "b", "c", "d", "e");
        HeroKitResolver resolver = new HeroKitResolver(
                Map.of("naruto-sage", sage),
                Map.of("naruto-sage", "awaken-skill-naruto-sage"));

        var normal = resolver.resolve("naruto-sage", false);
        var awakened = resolver.resolve("naruto-sage", true);

        assertEquals(5, normal.skills().size());
        assertEquals(6, awakened.skills().size());
        assertEquals("awaken-skill-naruto-sage", awakened.skills().get(5));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("naruto-uzumaki", false));
    }

    @Test
    void refusesAwakenedStateWhenHeroHasNoAwakeningSkill() {
        HeroKitDefinition adult = new HeroKitDefinition("sasuke-adult", "a", "b", "c", "d", "e");
        HeroKitResolver resolver = new HeroKitResolver(Map.of("sasuke-adult", adult), Map.of());
        assertThrows(IllegalStateException.class, () -> resolver.resolve("sasuke-adult", true));
    }
}
