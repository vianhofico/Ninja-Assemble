package com.ninjaassemble.hero.ownership;

import com.ninjaassemble.hero.catalog.HeroDefinitionSyncService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeroOwnershipService {
    private final JdbcTemplate jdbc;
    private final HeroDefinitionSyncService definitions;

    public HeroOwnershipService(JdbcTemplate jdbc, HeroDefinitionSyncService definitions) {
        this.jdbc = jdbc;
        this.definitions = definitions;
    }

    /**
     * Legacy compatibility entry point. BASE now resolves to one approved collectible Hero Version.
     */
    @Transactional
    public GrantResult grantBase(UUID playerId, String characterId) {
        String heroId = resolveLegacy(characterId, "__BASE__").heroId();
        if (heroId == null) {
            throw new IllegalArgumentException("character has no collectible Hero Version: " + characterId);
        }
        return grantHeroVersion(playerId, heroId);
    }

    /**
     * New ownership primitive: each approved Hero Version is independently collectible.
     */
    @Transactional
    public GrantResult grantHeroVersion(UUID playerId, String heroId) {
        HeroVersionIdentity identity = requireHeroVersion(heroId);
        definitions.ensureDefinition(identity.characterId());
        UUID generated = UUID.randomUUID();
        int inserted = jdbc.update("""
                insert into player_heroes(id, player_id, hero_definition_id, hero_version_id, awakened, awakening_level)
                values (?, ?, ?, ?, false, 0)
                on conflict (player_id, hero_version_id) do nothing
                """, generated, playerId, identity.characterId(), heroId);
        return new GrantResult(requireByHeroVersion(playerId, heroId), inserted == 1);
    }

    /**
     * Legacy variant compatibility. A collectible source form grants its Hero Version. An Awakening source form
     * permanently awakens that same owned Hero Version. Skill/skin/temp legacy rows never create extra heroes.
     */
    @Transactional
    public boolean unlockVariant(UUID playerId, String characterId, String variant) {
        if (variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE")) {
            return !grantBase(playerId, characterId).newHero() ? false : true;
        }
        LegacyResolution resolution = resolveLegacy(characterId, variant);
        if (resolution.heroId() == null) {
            throw new IllegalArgumentException("legacy content is not a collectible Hero Version: " + characterId + " / " + variant);
        }
        GrantResult grant = grantHeroVersion(playerId, resolution.heroId());
        OwnedHeroView owner = grant.hero();

        // Preserve the old unlock ledger only as an audit/compatibility artifact.
        int legacyUnlock = jdbc.update("""
                insert into hero_variant_unlocks(player_hero_id, variant_id, source)
                values (?, ?, 'M43_LEGACY_COMPATIBILITY')
                on conflict (player_hero_id, variant_id) do nothing
                """, owner.id(), variant);

        if (resolution.awakened()) {
            boolean changed = awaken(playerId, owner.id());
            return grant.newHero() || legacyUnlock == 1 || changed;
        }
        return grant.newHero() || legacyUnlock == 1;
    }

    /**
     * One-time persistent Awakening. There is no stage number and no chain.
     */
    @Transactional
    public boolean awaken(UUID playerId, UUID playerHeroId) {
        OwnedHeroView hero = requireOwned(playerId, playerHeroId);
        if (hero.awakened()) return false;
        Long count = jdbc.queryForObject("""
                select count(*)
                from hero_versions hv
                join awakening_definitions a on a.hero_id = hv.hero_id
                where hv.hero_id = ?
                """, Long.class, hero.heroId());
        if (count == null || count == 0) {
            throw new IllegalStateException("Hero Version has no Awakening: " + hero.heroId());
        }
        int changed = jdbc.update("""
                update player_heroes
                set awakened = true, awakened_at = coalesce(awakened_at, now()), awakening_level = 1,
                    current_variant_id = null
                where id = ? and player_id = ? and awakened = false
                """, playerHeroId, playerId);
        return changed == 1;
    }

    /**
     * Deprecated pre-M43 selector. It can only resolve ownership or activate the one Awakening; it no longer
     * transforms one collectible Hero Version into another.
     */
    @Transactional
    public OwnedHeroView selectVariant(UUID playerId, UUID playerHeroId, String variant) {
        OwnedHeroView current = requireOwned(playerId, playerHeroId);
        if (variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE")) {
            return current;
        }
        LegacyResolution resolution = resolveLegacy(current.characterId(), variant);
        if (resolution.heroId() == null) {
            throw new IllegalArgumentException("legacy content is not selectable as a Hero Version: " + variant);
        }
        if (!resolution.heroId().equals(current.heroId())) {
            // Old evolution used to mutate the current row. New model requires independent ownership instead.
            return requireByHeroVersion(playerId, resolution.heroId());
        }
        if (resolution.awakened()) awaken(playerId, playerHeroId);
        return requireOwned(playerId, playerHeroId);
    }

    @Transactional(readOnly = true)
    public List<OwnedHeroView> list(UUID playerId) {
        return jdbc.query(baseSelect() + " where ph.player_id = ? order by ph.level desc, hv.display_name_en asc",
                (rs, row) -> map(rs), playerId);
    }

    @Transactional(readOnly = true)
    public OwnedHeroView requireOwned(UUID playerId, UUID playerHeroId) {
        return jdbc.query(baseSelect() + " where ph.player_id = ? and ph.id = ?",
                (rs, row) -> map(rs), playerId, playerHeroId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned hero not found"));
    }

    @Transactional(readOnly = true)
    public OwnedHeroView requireByHeroVersion(UUID playerId, String heroId) {
        return jdbc.query(baseSelect() + " where ph.player_id = ? and ph.hero_version_id = ?",
                (rs, row) -> map(rs), playerId, heroId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned Hero Version not found: " + heroId));
    }

    /**
     * Legacy character lookup remains deterministic by preferring the approved BASE compatibility Hero Version.
     */
    @Transactional(readOnly = true)
    public OwnedHeroView requireByCharacter(UUID playerId, String characterId) {
        return jdbc.query(baseSelect() + """
                left join legacy_variant_hero_version_map primary_map
                  on primary_map.legacy_character_id = ph.hero_definition_id
                 and primary_map.legacy_variant_id = '__BASE__'
                where ph.player_id = ? and ph.hero_definition_id = ?
                order by case when ph.hero_version_id = primary_map.hero_version_id then 0 else 1 end,
                         ph.level desc, hv.display_name_en asc
                limit 1
                """, (rs, row) -> map(rs), playerId, characterId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned hero not found: " + characterId));
    }

    @Transactional(readOnly = true)
    public boolean hasHeroVersion(UUID playerId, String heroId) {
        Long count = jdbc.queryForObject(
                "select count(*) from player_heroes where player_id = ? and hero_version_id = ?",
                Long.class, playerId, heroId);
        return count != null && count > 0;
    }

    @Transactional(readOnly = true)
    public boolean hasVariant(UUID playerId, String characterId, String variant) {
        LegacyResolution resolution = resolveLegacy(characterId,
                variant == null || variant.isBlank() || variant.equalsIgnoreCase("BASE") ? "__BASE__" : variant);
        if (resolution.heroId() == null) return false;
        return jdbc.query("""
                select ph.awakened
                from player_heroes ph
                where ph.player_id = ? and ph.hero_version_id = ?
                """, (rs, row) -> rs.getBoolean(1), playerId, resolution.heroId()).stream().findFirst()
                .map(awakened -> !resolution.awakened() || awakened)
                .orElse(false);
    }

    private HeroVersionIdentity requireHeroVersion(String heroId) {
        return jdbc.query("""
                select hero_id, character_id, display_name_en
                from hero_versions
                where hero_id = ? and summonable = true
                """, (rs, row) -> new HeroVersionIdentity(rs.getString(1), rs.getString(2), rs.getString(3)), heroId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown/non-collectible Hero Version: " + heroId));
    }

    private LegacyResolution resolveLegacy(String characterId, String variant) {
        return jdbc.query("""
                select hero_version_id, awakened, mapping_kind
                from legacy_variant_hero_version_map
                where legacy_character_id = ? and legacy_variant_id = ?
                """, (rs, row) -> new LegacyResolution(rs.getString(1), rs.getBoolean(2), rs.getString(3)), characterId, variant)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown legacy variant mapping: " + characterId + " / " + variant));
    }

    private static String baseSelect() {
        return """
                select ph.id,
                       ph.hero_definition_id,
                       ph.hero_version_id,
                       hv.display_name_en as display_name,
                       ph.level,
                       ph.exp,
                       ph.frame_tier,
                       ph.awakened,
                       hv.awakening_id,
                       ad.name_en as awakening_name,
                       case
                         when ph.awakened then ad.name_en
                         when ph.current_variant_id is not null and btrim(ph.current_variant_id) <> '' then ph.current_variant_id
                         else base_map.legacy_variant_id
                       end as compatibility_variant,
                       case when ph.awakened then 1 else 0 end as compatibility_awakening_level,
                       ph.awakened_at
                from player_heroes ph
                join hero_versions hv on hv.hero_id = ph.hero_version_id
                left join awakening_definitions ad on ad.awakening_id = hv.awakening_id
                left join lateral (
                    select map.legacy_variant_id
                    from legacy_variant_hero_version_map map
                    where map.hero_version_id = hv.hero_id
                      and map.mapping_kind = 'COLLECTIBLE_HERO_VERSION'
                    order by map.legacy_variant_id
                    limit 1
                ) base_map on true
                """;
    }

    private static OwnedHeroView map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OwnedHeroView(
                rs.getObject("id", UUID.class),
                rs.getString("hero_definition_id"),
                rs.getString("hero_version_id"),
                rs.getString("display_name"),
                rs.getInt("level"),
                rs.getLong("exp"),
                rs.getString("frame_tier"),
                rs.getBoolean("awakened"),
                rs.getString("awakening_id"),
                rs.getString("awakening_name"),
                rs.getString("compatibility_variant"),
                rs.getInt("compatibility_awakening_level"));
    }

    private record HeroVersionIdentity(String heroId, String characterId, String displayName) {}
    private record LegacyResolution(String heroId, boolean awakened, String mappingKind) {}
    public record GrantResult(OwnedHeroView hero, boolean newHero) {}
}
