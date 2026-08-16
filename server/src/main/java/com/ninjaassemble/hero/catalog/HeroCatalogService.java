package com.ninjaassemble.hero.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HeroCatalogService {
    private final List<HeroCatalogEntry> entries;

    public HeroCatalogService(ObjectMapper objectMapper) {
        try (InputStream input = HeroCatalogService.class.getResourceAsStream("/content/roster-base.json")) {
            if (input == null) throw new IllegalStateException("missing roster-base.json");
            entries = List.copyOf(objectMapper.readValue(input, new TypeReference<List<HeroCatalogEntry>>() {}));
        } catch (IOException e) {
            throw new IllegalStateException("cannot load hero catalog", e);
        }
    }

    public List<HeroCatalogEntry> all() { return entries; }

    public List<HeroCatalogEntry> byGroup(String group) {
        if (group == null || group.isBlank()) return entries;
        return entries.stream().filter(it -> it.group().equalsIgnoreCase(group)).toList();
    }
}
