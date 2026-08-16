package com.ninjaassemble.economy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletBalanceTest {
    @Test
    void balanceNeverGoesNegative() {
        WalletBalance balance = new WalletBalance(new WalletBalanceId(UUID.randomUUID(), Currency.GOLD));
        balance.apply(100);
        assertEquals(60, balance.apply(-40).after());
        assertThrows(IllegalStateException.class, () -> balance.apply(-61));
    }
}
