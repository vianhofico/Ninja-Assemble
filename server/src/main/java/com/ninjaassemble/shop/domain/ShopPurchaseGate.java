package com.ninjaassemble.shop.domain;

public final class ShopPurchaseGate {
    private ShopPurchaseGate() {}

    public static Result evaluate(ShopOffer offer, long balance, int purchasedCount) {
        if (offer == null || balance < 0 || purchasedCount < 0) throw new IllegalArgumentException("invalid purchase state");
        if (balance < offer.price()) return new Result(false, "insufficient-currency");
        if (offer.purchaseLimit() != null && purchasedCount >= offer.purchaseLimit()) return new Result(false, "purchase-limit");
        return new Result(true, null);
    }

    public record Result(boolean allowed, String reason) {}
}
