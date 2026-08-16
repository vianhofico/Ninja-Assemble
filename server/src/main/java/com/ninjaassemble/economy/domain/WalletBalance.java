package com.ninjaassemble.economy.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "wallet_balances")
public class WalletBalance {
    @EmbeddedId
    private WalletBalanceId id;
    private long amount;

    protected WalletBalance() {}
    public WalletBalance(WalletBalanceId id) { this.id = id; this.amount = 0; }
    public WalletBalanceId getId() { return id; }
    public long getAmount() { return amount; }

    public WalletMutation apply(long delta) {
        long before = amount;
        long after = Math.addExact(before, delta);
        if (after < 0) throw new IllegalStateException("insufficient balance");
        amount = after;
        return new WalletMutation(before, after, delta);
    }

    public record WalletMutation(long before, long after, long delta) {}
}
