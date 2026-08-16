package com.ninjaassemble.economy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_ledger")
public class WalletLedgerEntry {
    @Id private UUID id;
    @Column(name = "player_id", nullable = false) private UUID playerId;
    @Column(nullable = false, length = 32) private String currency;
    @Column(nullable = false) private long delta;
    @Column(name = "balance_before", nullable = false) private long balanceBefore;
    @Column(name = "balance_after", nullable = false) private long balanceAfter;
    @Column(nullable = false, length = 64) private String reason;
    @Column(length = 128) private String source;
    @Column(name = "idempotency_key", length = 160) private String idempotencyKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected WalletLedgerEntry() {}
    public WalletLedgerEntry(UUID playerId, Currency currency, WalletBalance.WalletMutation mutation, String reason, String source, String idempotencyKey, Instant now) {
        this.id = UUID.randomUUID(); this.playerId = playerId; this.currency = currency.name(); this.delta = mutation.delta();
        this.balanceBefore = mutation.before(); this.balanceAfter = mutation.after(); this.reason = reason; this.source = source;
        this.idempotencyKey = idempotencyKey; this.createdAt = now;
    }
}
