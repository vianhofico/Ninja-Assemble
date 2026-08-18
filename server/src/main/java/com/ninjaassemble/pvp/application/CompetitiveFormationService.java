package com.ninjaassemble.pvp.application;

import com.ninjaassemble.battle.domain.BattleRules;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import com.ninjaassemble.play.application.FormationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class CompetitiveFormationService {
    private static final String ARENA_MODE = "ARENA";
    private final JdbcTemplate jdbc;
    private final HeroOwnershipService ownership;
    private final FormationService campaignFormation;

    public CompetitiveFormationService(JdbcTemplate jdbc, HeroOwnershipService ownership, FormationService campaignFormation) {
        this.jdbc = jdbc; this.ownership = ownership; this.campaignFormation = campaignFormation;
    }

    @Transactional
    public ArenaDefense saveArenaDefense(UUID playerId, List<UUID> heroIds) {
        validateOwned(playerId, heroIds, 5, "Arena defense");
        UUID formationId = jdbc.query("""
                select id from formations where player_id=? and mode=? and squad_index=0 and is_defense=true
                """, (rs, row) -> rs.getObject(1, UUID.class), playerId, ARENA_MODE).stream().findFirst().orElse(null);
        if (formationId == null) {
            formationId = UUID.randomUUID();
            jdbc.update("insert into formations(id, player_id, mode, squad_index, is_defense) values (?, ?, ?, 0, true)",
                    formationId, playerId, ARENA_MODE);
        }
        replaceSlots(formationId, heroIds);
        jdbc.update("update arena_profiles set defense_formation_id=?, updated_at=now() where player_id=?", formationId, playerId);
        return new ArenaDefense(formationId, views(playerId, heroIds));
    }

    @Transactional
    public ArenaDefense ensureArenaDefense(UUID playerId) {
        ArenaDefense existing = loadArenaDefense(playerId);
        if (existing.heroes().size() == 5) return existing;
        FormationService.FormationView current = campaignFormation.load(playerId);
        if (current.heroes().size() != 5) return new ArenaDefense(null, List.of());
        return saveArenaDefense(playerId, current.heroes().stream().map(OwnedHeroView::id).toList());
    }

    @Transactional(readOnly = true)
    public ArenaDefense loadArenaDefense(UUID playerId) {
        List<FormationRow> formations = jdbc.query("""
                select id from formations where player_id=? and mode=? and squad_index=0 and is_defense=true
                """, (rs, row) -> new FormationRow(rs.getObject(1, UUID.class)), playerId, ARENA_MODE);
        if (formations.isEmpty()) return new ArenaDefense(null, List.of());
        UUID id = formations.get(0).id();
        return new ArenaDefense(id, views(playerId, slotIds(id)));
    }

    @Transactional
    public ShadowDefense saveShadowDefense(UUID playerId, String seasonId, List<UUID> heroIds) {
        validateOwned(playerId, heroIds, BattleRules.SHADOW_ROSTER_SIZE, "Shadow Arena defense");
        jdbc.update("delete from shadow_defense_formations where player_id=? and season_id=?", playerId, seasonId);
        for (int index = 0; index < heroIds.size(); index++) {
            jdbc.update("""
                    insert into shadow_defense_formations(player_id, season_id, squad_index, slot_index, player_hero_id, updated_at)
                    values (?, ?, ?, ?, ?, now())
                    """, playerId, seasonId, index / 5, index % 5, heroIds.get(index));
        }
        return new ShadowDefense(seasonId, views(playerId, heroIds));
    }

    @Transactional
    public ShadowDefense ensureShadowDefense(UUID playerId, String seasonId) {
        ShadowDefense existing = loadShadowDefense(playerId, seasonId);
        if (existing.heroes().size() == BattleRules.SHADOW_ROSTER_SIZE) return existing;
        List<OwnedHeroView> owned = ownership.list(playerId);
        if (owned.size() < BattleRules.SHADOW_ROSTER_SIZE) return new ShadowDefense(seasonId, List.of());
        List<UUID> ids = owned.subList(0, BattleRules.SHADOW_ROSTER_SIZE).stream().map(OwnedHeroView::id).toList();
        return saveShadowDefense(playerId, seasonId, ids);
    }

    @Transactional(readOnly = true)
    public ShadowDefense loadShadowDefense(UUID playerId, String seasonId) {
        List<UUID> ids = jdbc.query("""
                select player_hero_id from shadow_defense_formations
                where player_id=? and season_id=? order by squad_index, slot_index
                """, (rs, row) -> rs.getObject(1, UUID.class), playerId, seasonId);
        return new ShadowDefense(seasonId, views(playerId, ids));
    }

    private void replaceSlots(UUID formationId, List<UUID> heroIds) {
        jdbc.update("delete from formation_slots where formation_id=?", formationId);
        for (int i = 0; i < heroIds.size(); i++)
            jdbc.update("insert into formation_slots(formation_id, slot_index, player_hero_id) values (?, ?, ?)", formationId, i, heroIds.get(i));
    }

    private List<UUID> slotIds(UUID formationId) {
        return jdbc.query("select player_hero_id from formation_slots where formation_id=? order by slot_index",
                (rs, row) -> rs.getObject(1, UUID.class), formationId);
    }

    private void validateOwned(UUID playerId, List<UUID> heroIds, int expected, String label) {
        if (heroIds == null || heroIds.size() != expected) throw new IllegalArgumentException(label + " requires " + expected + " ninja");
        if (new HashSet<>(heroIds).size() != heroIds.size()) throw new IllegalArgumentException(label + " cannot contain duplicates");
        for (UUID id : heroIds) ownership.requireOwned(playerId, id);
    }

    private List<OwnedHeroView> views(UUID playerId, List<UUID> ids) {
        List<OwnedHeroView> result = new ArrayList<>();
        for (UUID id : ids) result.add(ownership.requireOwned(playerId, id));
        return List.copyOf(result);
    }

    private record FormationRow(UUID id) { }
    public record ArenaDefense(UUID formationId, List<OwnedHeroView> heroes) { }
    public record ShadowDefense(String seasonId, List<OwnedHeroView> heroes) { }
}
