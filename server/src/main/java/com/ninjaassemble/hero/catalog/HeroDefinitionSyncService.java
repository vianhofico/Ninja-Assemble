package com.ninjaassemble.hero.catalog;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeroDefinitionSyncService {
    private final JdbcTemplate jdbc;
    private final HeroCatalogService catalog;

    public HeroDefinitionSyncService(JdbcTemplate jdbc, HeroCatalogService catalog) {
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void syncAll() {
        for (HeroCatalogEntry hero : catalog.all()) ensureDefinition(hero.id());
    }

    public void ensureDefinition(String characterId) {
        HeroCatalogEntry hero = catalog.require(characterId);
        jdbc.update("""
                insert into hero_definitions(id, display_name, variant, archetype, definition_version, parity_status, content)
                values (?, ?, null, 'UNKNOWN', 1, 'IMPLEMENTED', '{}'::jsonb)
                on conflict (id) do update set display_name = excluded.display_name
                """, hero.id(), hero.character());
    }
}
