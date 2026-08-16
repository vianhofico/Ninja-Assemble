package com.ninjaassemble.economy.application;

import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.economy.domain.WalletBalance;
import com.ninjaassemble.economy.domain.WalletBalanceId;
import com.ninjaassemble.economy.domain.WalletBalanceRepository;
import com.ninjaassemble.economy.domain.WalletLedgerEntry;
import com.ninjaassemble.economy.domain.WalletLedgerRepository;
import com.ninjaassemble.player.domain.PlayerRepository;
import java.time.Clock;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private final WalletBalanceRepository balances;
    private final WalletLedgerRepository ledger;
    private final PlayerRepository players;
    private final Clock clock;

    public WalletService(WalletBalanceRepository balances, WalletLedgerRepository ledger, PlayerRepository players, Clock clock) {
        this.balances = balances; this.ledger = ledger; this.players = players; this.clock = clock;
    }

    @Transactional
    public long mutate(UUID playerId, Currency currency, long delta, String reason, String source, String idempotencyKey) {
        if (!players.existsById(playerId)) throw new IllegalArgumentException("player not found");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("reason required");
        if (idempotencyKey != null && ledger.existsByPlayerIdAndIdempotencyKey(playerId, idempotencyKey)) {
            return getBalance(playerId, currency);
        }
        WalletBalanceId id = new WalletBalanceId(playerId, currency);
        WalletBalance balance = balances.findWithLockById(id).orElseGet(() -> new WalletBalance(id));
        WalletBalance.WalletMutation mutation = balance.apply(delta);
        balances.save(balance);
        ledger.save(new WalletLedgerEntry(playerId, currency, mutation, reason, source, idempotencyKey, clock.instant()));
        return mutation.after();
    }

    @Transactional(readOnly = true)
    public long getBalance(UUID playerId, Currency currency) {
        return balances.findById(new WalletBalanceId(playerId, currency)).map(WalletBalance::getAmount).orElse(0L);
    }

    @Transactional(readOnly = true)
    public Map<Currency, Long> snapshot(UUID playerId) {
        Map<Currency, Long> result = new EnumMap<>(Currency.class);
        for (Currency currency : Currency.values()) result.put(currency, getBalance(playerId, currency));
        return result;
    }
}
