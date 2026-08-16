package com.ninjaassemble.shop.domain;

public record ShopOffer(
        String id,
        String itemDefinitionId,
        long quantity,
        String currency,
        long price,
        Integer purchaseLimit
) {
    public ShopOffer {
        if (id == null || id.isBlank() || itemDefinitionId == null || itemDefinitionId.isBlank() || quantity <= 0 || currency == null || currency.isBlank() || price < 0) throw new IllegalArgumentException("invalid shop offer");
        if (purchaseLimit != null && purchaseLimit < 1) throw new IllegalArgumentException("invalid purchase limit");
    }
}
