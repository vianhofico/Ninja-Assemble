package com.ninjaassemble.guild.domain;

import java.util.UUID;

public record GuildBossState(String bossDefinitionId, long maxHp, long currentHp, long totalDamage) {
    public GuildBossState {
        if (bossDefinitionId == null || bossDefinitionId.isBlank() || maxHp <= 0 || currentHp < 0 || currentHp > maxHp || totalDamage < 0) throw new IllegalArgumentException("invalid guild boss state");
    }

    public DamageResult apply(UUID playerId, long damage) {
        if (playerId == null || damage <= 0) throw new IllegalArgumentException("invalid damage");
        long applied = Math.min(currentHp, damage);
        return new DamageResult(new GuildBossState(bossDefinitionId, maxHp, currentHp - applied, Math.addExact(totalDamage, applied)), playerId, applied);
    }

    public record DamageResult(GuildBossState state, UUID playerId, long appliedDamage) {}
}
