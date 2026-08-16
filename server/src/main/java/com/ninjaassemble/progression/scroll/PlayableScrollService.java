package com.ninjaassemble.progression.scroll;

import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.play.application.ActionRequestService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayableScrollService {
    private static final String ACTION = "SCROLL_COMBINE";
    private static final Set<String> STARTERS = Set.of(
            "scroll-yinyang-power", "scroll-fire-attack", "scroll-water-defense",
            "scroll-wind-speed", "scroll-earth-defense", "scroll-lightning-attack");

    private final JdbcTemplate jdbc;
    private final PlayableScrollCatalogService catalog;
    private final HeroOwnershipService ownership;
    private final ActionRequestService requests;

    public PlayableScrollService(JdbcTemplate jdbc, PlayableScrollCatalogService catalog,
                                 HeroOwnershipService ownership, ActionRequestService requests) {
        this.jdbc = jdbc; this.catalog = catalog; this.ownership = ownership; this.requests = requests;
    }

    @Transactional
    public List<OwnedScrollView> grantStarterSet(UUID playerId) {
        for (String definitionId : STARTERS) {
            catalog.require(definitionId);
            Long count = jdbc.queryForObject("select count(*) from player_college_scrolls where player_id = ? and scroll_definition_id = ?",
                    Long.class, playerId, definitionId);
            if (count == null || count == 0) {
                jdbc.update("insert into player_college_scrolls(id, player_id, scroll_definition_id, level) values (?, ?, ?, 1)",
                        UUID.randomUUID(), playerId, definitionId);
            }
        }
        return list(playerId);
    }

    @Transactional(readOnly = true)
    public List<OwnedScrollView> list(UUID playerId) {
        return jdbc.query("""
                select ps.id, ps.scroll_definition_id, sd.name_key, sd.element, ps.level, sd.max_level,
                       slots.player_hero_id
                from player_college_scrolls ps
                join scroll_definitions sd on sd.id = ps.scroll_definition_id
                left join player_college_scroll_slots slots on slots.player_scroll_id = ps.id
                where ps.player_id = ?
                order by sd.element, ps.level desc, sd.id
                """, (rs, row) -> new OwnedScrollView(
                rs.getObject("id", UUID.class), rs.getString("scroll_definition_id"), rs.getString("name_key"),
                rs.getString("element"), rs.getInt("level"), rs.getInt("max_level"),
                rs.getObject("player_hero_id", UUID.class)), playerId);
    }

    @Transactional
    public OwnedScrollView inlay(UUID playerId, UUID playerHeroId, UUID playerScrollId) {
        ownership.requireOwned(playerId, playerHeroId);
        OwnedScrollView scroll = requireOwned(playerId, playerScrollId);
        jdbc.update("delete from player_college_scroll_slots where player_scroll_id = ?", playerScrollId);
        jdbc.update("""
                insert into player_college_scroll_slots(player_hero_id, element, player_scroll_id)
                values (?, ?, ?)
                on conflict (player_hero_id, element) do update set player_scroll_id = excluded.player_scroll_id
                """, playerHeroId, scroll.element(), playerScrollId);
        return requireOwned(playerId, playerScrollId);
    }

    @Transactional
    public CombineResult combine(UUID playerId, String definitionId, int level, UUID requestId) {
        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) return decode(playerId, existing.get());
        requests.reserve(playerId, requestId, ACTION);
        PlayableScrollCatalogService.ScrollView definition = catalog.require(definitionId);
        if (level < 1 || level >= definition.maxLevel()) throw new IllegalStateException("scroll cannot be combined at this level");
        List<UUID> copies = jdbc.query("""
                select ps.id
                from player_college_scrolls ps
                left join player_college_scroll_slots slots on slots.player_scroll_id = ps.id
                where ps.player_id = ? and ps.scroll_definition_id = ? and ps.level = ? and slots.player_scroll_id is null
                order by ps.created_at, ps.id
                limit 3
                """, (rs, row) -> rs.getObject(1, UUID.class), playerId, definitionId, level);
        if (copies.size() < 3) throw new IllegalStateException("three unequipped duplicate scrolls are required");
        for (UUID id : copies) jdbc.update("delete from player_college_scrolls where player_id = ? and id = ?", playerId, id);
        UUID upgradedId = UUID.randomUUID();
        jdbc.update("insert into player_college_scrolls(id, player_id, scroll_definition_id, level) values (?, ?, ?, ?)",
                upgradedId, playerId, definitionId, level + 1);
        CombineResult result = new CombineResult(requireOwned(playerId, upgradedId), 3, "scroll-combine-experimental-v1");
        requests.complete(playerId, requestId, upgradedId + "\t3");
        return result;
    }

    @Transactional(readOnly = true)
    public OwnedScrollView requireOwned(UUID playerId, UUID playerScrollId) {
        return jdbc.query("""
                select ps.id, ps.scroll_definition_id, sd.name_key, sd.element, ps.level, sd.max_level,
                       slots.player_hero_id
                from player_college_scrolls ps
                join scroll_definitions sd on sd.id = ps.scroll_definition_id
                left join player_college_scroll_slots slots on slots.player_scroll_id = ps.id
                where ps.player_id = ? and ps.id = ?
                """, (rs, row) -> new OwnedScrollView(
                rs.getObject("id", UUID.class), rs.getString("scroll_definition_id"), rs.getString("name_key"),
                rs.getString("element"), rs.getInt("level"), rs.getInt("max_level"),
                rs.getObject("player_hero_id", UUID.class)), playerId, playerScrollId).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("owned scroll not found"));
    }

    private CombineResult decode(UUID playerId, String stored) {
        String[] p = stored.split("\t", -1);
        if (p.length != 2) throw new IllegalStateException("corrupt stored scroll combine response");
        return new CombineResult(requireOwned(playerId, UUID.fromString(p[0])), Integer.parseInt(p[1]), "scroll-combine-experimental-v1");
    }

    public record OwnedScrollView(UUID id, String definitionId, String nameKey, String element,
                                  int level, int maxLevel, UUID equippedPlayerHeroId) {}
    public record CombineResult(OwnedScrollView upgraded, int copiesConsumed, String profileVersion) {}
}
