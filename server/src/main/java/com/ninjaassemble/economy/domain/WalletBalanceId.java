package com.ninjaassemble.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class WalletBalanceId implements Serializable {
    @Column(name = "player_id")
    private UUID playerId;
    @Column(name = "currency", length = 32)
    private String currency;

    protected WalletBalanceId() {}
    public WalletBalanceId(UUID playerId, Currency currency) { this.playerId = playerId; this.currency = currency.name(); }
    public UUID getPlayerId() { return playerId; }
    public String getCurrency() { return currency; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WalletBalanceId that)) return false;
        return Objects.equals(playerId, that.playerId) && Objects.equals(currency, that.currency);
    }
    @Override public int hashCode() { return Objects.hash(playerId, currency); }
}
