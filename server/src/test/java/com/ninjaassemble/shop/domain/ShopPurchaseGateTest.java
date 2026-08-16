package com.ninjaassemble.shop.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ShopPurchaseGateTest {
    @Test
    void validatesCurrencyAndPurchaseLimit() {
        ShopOffer offer = new ShopOffer("o", "item", 1, "ARENA_COIN", 100, 2);
        assertFalse(ShopPurchaseGate.evaluate(offer, 99, 0).allowed());
        assertFalse(ShopPurchaseGate.evaluate(offer, 100, 2).allowed());
        assertTrue(ShopPurchaseGate.evaluate(offer, 100, 1).allowed());
    }
}
