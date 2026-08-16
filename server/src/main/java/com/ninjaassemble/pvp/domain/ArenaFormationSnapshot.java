package com.ninjaassemble.pvp.domain;

import com.ninjaassemble.battle.domain.BattleRules;
import java.util.HashSet;
import java.util.List;

public record ArenaFormationSnapshot(List<FormationMemberSnapshot> members) {
    public ArenaFormationSnapshot {
        if (members == null) throw new IllegalArgumentException("members required");
        members = List.copyOf(members);
        BattleRules.requireArenaTeamSize(members.size());
        var ids = new HashSet<String>();
        for (FormationMemberSnapshot member : members) if (!ids.add(member.playerHeroId())) throw new IllegalArgumentException("duplicate hero in formation");
    }

    public long totalPower() { return members.stream().mapToLong(FormationMemberSnapshot::power).sum(); }
}
