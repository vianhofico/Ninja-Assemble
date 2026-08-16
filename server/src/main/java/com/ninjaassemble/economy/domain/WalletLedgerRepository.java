package com.ninjaassemble.economy.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntry, UUID> {
    boolean existsByPlayerIdAndIdempotencyKey(UUID playerId, String idempotencyKey);
}
