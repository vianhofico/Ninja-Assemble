package com.ninjaassemble.play.domain;

import com.ninjaassemble.battle.sim.TeamSide;

public record BattleParticipant(
        String battleUnitId,
        String characterId,
        String displayName,
        String variant,
        int level,
        TeamSide side,
        int slot,
        long maxHp
) {
    public BattleParticipant {
        if (battleUnitId == null || battleUnitId.isBlank()) throw new IllegalArgumentException("battleUnitId required");
        if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("characterId required");
        if (displayName == null || displayName.isBlank()) displayName = characterId;
        if (level <= 0) throw new IllegalArgumentException("level must be positive");
        if (side == null) throw new IllegalArgumentException("side required");
        if (slot < 0 || slot > 4) throw new IllegalArgumentException("slot must be between 0 and 4");
        if (maxHp <= 0) throw new IllegalArgumentException("maxHp must be positive");
        variant = variant == null ? "" : variant;
    }
}
