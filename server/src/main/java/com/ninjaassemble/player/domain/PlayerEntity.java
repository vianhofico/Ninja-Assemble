package com.ninjaassemble.player.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "players")
public class PlayerEntity {
    @Id
    private UUID id;
    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;
    @Column(name = "guest_key", unique = true, length = 96)
    private String guestKey;
    @Column(name = "account_level", nullable = false)
    private int accountLevel;
    @Column(name = "account_exp", nullable = false)
    private long accountExp;
    @Column(nullable = false)
    private int energy;
    @Column(name = "energy_cap", nullable = false)
    private int energyCap;
    @Column(name = "energy_updated_at", nullable = false)
    private Instant energyUpdatedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PlayerEntity() {}

    public PlayerEntity(UUID id, String displayName, String guestKey, int energyCap, Instant now) {
        this.id = id;
        this.displayName = displayName;
        this.guestKey = guestKey;
        this.accountLevel = 1;
        this.accountExp = 0;
        this.energy = energyCap;
        this.energyCap = energyCap;
        this.energyUpdatedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getGuestKey() { return guestKey; }
    public int getAccountLevel() { return accountLevel; }
    public long getAccountExp() { return accountExp; }
    public int getEnergy() { return energy; }
    public int getEnergyCap() { return energyCap; }
    public Instant getEnergyUpdatedAt() { return energyUpdatedAt; }

    public void rename(String value, Instant now) { this.displayName = value; this.updatedAt = now; }
    public void setEnergyState(int value, Instant updatedAt) {
        if (value < 0 || value > energyCap) throw new IllegalArgumentException("energy outside range");
        this.energy = value;
        this.energyUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }
    public void addAccountExp(long amount, long expPerLevel, Instant now) {
        if (amount < 0 || expPerLevel <= 0) throw new IllegalArgumentException("invalid exp update");
        accountExp += amount;
        while (accountExp >= expPerLevel) {
            accountExp -= expPerLevel;
            accountLevel++;
        }
        updatedAt = now;
    }
}
