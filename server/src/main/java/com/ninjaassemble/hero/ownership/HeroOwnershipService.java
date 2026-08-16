package com.ninjaassemble.hero.ownership;

import com.ninjaassemble.hero.catalog.HeroDefinitionSyncService;
import com.ninjaassemble.hero.catalog.VariantCatalogService;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeroOwnershipService {
    private final JdbcTemplate jdbc;
    private final HeroDefinitionSyncService definitions;
    private final VariantCatalogService variants;

    public HeroOwnershipService(JdbcTemplate jdbc, HeroDefinitionSyncService definitions, VariantCatalogService variants) {
        this.jdbc = jdbc;
        this.definitions = definitions;
        this.variants = variants;
    }

    @Transactional
    public GrantResult grantBase(UUID playerId, String characterId) {
        definitions.ensureDefinition(characterId);
        UUID generated = UUID.randomUUID();
        int inserted = jdbc.update("""
                insert into player_heroes(id, player_id, hero_definition_id)
                values (?, ?, ?)
                on conflict (player_id, hero_definition_id) do nothing
                """, generated, playerId, characterId);
        return new GrantResult(requireByCharacter(playerId, characterId), inserted == 1);
    }

    @Transactional
    public boolean unlockVariant(UUID playerId, String characterId, String variant) {
        if (variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE")) return false;
        boolean exists = variants.forCharacter(characterId).stream().anyMatch(it -> it.variant().equals(variant));
        if (!exists) throw new IllegalArgumentException("unknown variant for character: " + characterId + " / " + variant);
        GrantResult base = grantBase(playerId, characterId);
        int inserted = jdbc.update("""
                insert into hero_variant_unlocks(player_hero_id, variant_id, source)
                values (?, ?, 'SUMMON_OR_PROGRESS')
                on conflict (player_hero_id, variant_id) do nothing
                """, base.hero().id(), variant);
        return inserted == 1;
    }

    @Transactional
    public OwnedHeroView selectVariant(UUID playerId, UUID playerHeroId, String variant) {
        requireOwned(playerId, playerHeroId);
        if (variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE")) {
            jdbc.update("update player_heroes set current_variant_id = null where id = ? and player_id = ?", playerHeroId, playerId);
            return requireOwned(playerId, playerHeroId);
        }
        Long count = jdbc.queryForObject("select count(*) from hero_variant_unlocks where player_hero_id = ? and variant_id = ?", Long.class, playerHeroId, variant);
        if (count == null || count == 0) throw new IllegalStateException("variant is not unlocked");
        jdbc.update("update player_heroes set current_variant_id = ? where id = ? and player_id = ?", variant, playerHeroId, playerId);
        return requireOwned(playerId, playerHeroId);
    }

    @Transactional(readOnly = true)
    public List<OwnedHeroView> list(UUID playerId) {
        return jdbc.query("""
                select ph.id, ph.hero_definition_id, hd.display_name, ph.level, ph.exp, ph.frame_tier,
                       ph.current_variant_id, ph.awakening_level
                from player_heroes ph
                join hero_definitions hd on hd.id = ph.hero_definition_id
                where ph.player_id = ?
                order by ph.level desc, hd.display_name asc
                """, (rs, row) -> map(rs), playerId);
    }

    @Transactional(readOnly = true)
    public OwnedHeroView requireOwned(UUID playerId, UUID playerHeroId) {
        return jdbc.query("""
                select ph.id, ph.hero_definition_id, hd.display_name, ph.level, ph.exp, ph.frame_tier,
                       ph.current_variant_id, ph.awakening_level
                from player_heroes ph
                join hero_definitions hd on hd.id = ph.hero_definition_id
                where ph.player_id = ? and ph.id = ?
                """, (rs, row) -> map(rs), playerId, playerHeroId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned hero not found"));
    }

    @Transactional(readOnly = true)
    public OwnedHeroView requireByCharacter(UUID playerId, String characterId) {
        return jdbc.query("""
                select ph.id, ph.hero_definition_id, hd.display_name, ph.level, ph.exp, ph.frame_tier,
                       ph.current_variant_id, ph.awakening_level
                from player_heroes ph
                join hero_definitions hd on hd.id = ph.hero_definition_id
                where ph.player_id = ? and ph.hero_definition_id = ?
                """, (rs, row) -> map(rs), playerId, characterId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned hero not found: " + characterId));
    }

    @Transactional(readOnly = true)
    public boolean hasVariant(UUID playerId, String characterId, String variant) {
        if (variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE")) {
            return !jdbc.query("select id from player_heroes where player_id = ? and hero_definition_id = ?", (rs, row) -> rs.getObject(1, UUID.class), playerId, characterId).isEmpty();
        }
        Long count = jdbc.queryForObject("""
                select count(*) from hero_variant_unlocks u
                join player_heroes ph on ph.id = u.player_hero_id
                where ph.player_id = ? and ph.hero_definition_id = ? and u.variant_id = ?
                """, Long.class, playerId, characterId, variant);
        return count != null && count > 0;
    }

    private static OwnedHeroView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OwnedHeroView(
                rs.getObject("id", UUID.class), rs.getString("hero_definition_id"), rs.getString("display_name"),
                rs.getInt("level"), rs.getLong("exp"), rs.getString("frame_tier"), rs.getString("current_variant_id"),
                rs.getInt("awakening_level"));
    }

    public record GrantResult(OwnedHeroView hero, boolean newHero) {}
}
