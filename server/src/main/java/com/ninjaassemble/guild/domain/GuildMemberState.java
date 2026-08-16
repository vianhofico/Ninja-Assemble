package com.ninjaassemble.guild.domain;

import java.util.UUID;

public record GuildMemberState(UUID playerId, GuildRole role, long contribution) {
    public GuildMemberState {
        if (playerId == null || role == null || contribution < 0) throw new IllegalArgumentException("invalid guild member");
    }

    public GuildMemberState contribute(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("contribution must be positive");
        return new GuildMemberState(playerId, role, Math.addExact(contribution, amount));
    }
}
