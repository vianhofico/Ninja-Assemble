package com.ninjaassemble.progression.scroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PlayableScrollCatalogServiceTest {
    @Test
    void catalogCoversYinYangAndAllFiveElements() {
        PlayableScrollCatalogService catalog = new PlayableScrollCatalogService(null);
        assertEquals(18, catalog.all().size());
        Set<String> elements = catalog.all().stream().map(PlayableScrollCatalogService.ScrollView::element).collect(Collectors.toSet());
        assertEquals(Set.of("YIN_YANG", "FIRE", "WATER", "WIND", "EARTH", "LIGHTNING"), elements);
        assertEquals(10, catalog.all().stream().mapToInt(PlayableScrollCatalogService.ScrollView::maxLevel).max().orElseThrow());
    }
}
