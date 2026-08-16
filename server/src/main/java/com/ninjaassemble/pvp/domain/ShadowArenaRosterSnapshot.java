package com.ninjaassemble.pvp.domain;

import com.ninjaassemble.battle.domain.BattleRules;
import java.util.HashSet;
import java.util.List;

public record ShadowArenaRosterSnapshot(List<ArenaFormationSnapshot> squads) {
    public ShadowArenaRosterSnapshot {
        if (squads == null || squads.size() != BattleRules.SHADOW_SQUAD_COUNT) throw new IllegalArgumentException("Shadow Arena requires exactly three squads");
        squads = List.copyOf(squads);
        var heroIds = new HashSet<String>();
        for (ArenaFormationSnapshot squad : squads) {
            BattleRules.requireShadowSquadSize(squad.members().size());
            for (FormationMemberSnapshot member : squad.members()) if (!heroIds.add(member.playerHeroId())) throw new IllegalArgumentException("Shadow Arena roster cannot reuse a hero across squads");
        }
        BattleRules.requireShadowRosterSize(heroIds.size());
    }

    public long totalPower() { return squads.stream().mapToLong(ArenaFormationSnapshot::totalPower).sum(); }
}
