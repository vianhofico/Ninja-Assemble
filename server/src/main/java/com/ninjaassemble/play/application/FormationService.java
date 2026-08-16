package com.ninjaassemble.play.application;

import com.ninjaassemble.battle.domain.BattleRules;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.hero.ownership.OwnedHeroView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormationService {
    private static final String MODE = "CAMPAIGN";
    private final JdbcTemplate jdbc;
    private final HeroOwnershipService ownership;

    public FormationService(JdbcTemplate jdbc, HeroOwnershipService ownership) {
        this.jdbc = jdbc;
        this.ownership = ownership;
    }

    @Transactional
    public FormationView save(UUID playerId, List<UUID> playerHeroIds) {
        if (playerHeroIds == null) throw new IllegalArgumentException("formation is required");
        BattleRules.requireArenaTeamSize(playerHeroIds.size());
        if (new HashSet<>(playerHeroIds).size() != playerHeroIds.size()) throw new IllegalArgumentException("formation cannot contain duplicate heroes");
        for (UUID heroId : playerHeroIds) ownership.requireOwned(playerId, heroId);

        UUID formationId = jdbc.query("""
                select id from formations where player_id = ? and mode = ? and squad_index = 0 and is_defense = false
                """, (rs, row) -> rs.getObject(1, UUID.class), playerId, MODE).stream().findFirst().orElse(null);
        if (formationId == null) {
            formationId = UUID.randomUUID();
            jdbc.update("insert into formations(id, player_id, mode, squad_index, is_defense) values (?, ?, ?, 0, false)", formationId, playerId, MODE);
        }
        jdbc.update("delete from formation_slots where formation_id = ?", formationId);
        for (int i = 0; i < playerHeroIds.size(); i++) {
            jdbc.update("insert into formation_slots(formation_id, slot_index, player_hero_id) values (?, ?, ?)", formationId, i, playerHeroIds.get(i));
        }
        return load(playerId);
    }

    @Transactional(readOnly = true)
    public FormationView load(UUID playerId) {
        List<UUID> ids = jdbc.query("""
                select fs.player_hero_id
                from formations f join formation_slots fs on fs.formation_id = f.id
                where f.player_id = ? and f.mode = ? and f.squad_index = 0 and f.is_defense = false
                order by fs.slot_index
                """, (rs, row) -> rs.getObject(1, UUID.class), playerId, MODE);
        List<OwnedHeroView> heroes = new ArrayList<>();
        for (UUID id : ids) heroes.add(ownership.requireOwned(playerId, id));
        return new FormationView(heroes);
    }

    public record FormationView(List<OwnedHeroView> heroes) {}
}
