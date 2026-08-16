package com.ninjaassemble.progression.tailedbeast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JinchurikiRegistryTest {
    @Test
    void registryMapsHostToBeast() {
        JinchurikiRegistry registry = new JinchurikiRegistry(List.of(
                new JinchurikiDefinition("naruto", TailedBeast.KURAMA, Set.of("kcm1", "kcm2"), "kurama")));
        assertEquals(TailedBeast.KURAMA, registry.require("naruto").beast());
    }
}
