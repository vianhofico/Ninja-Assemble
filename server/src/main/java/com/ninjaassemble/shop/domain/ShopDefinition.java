package com.ninjaassemble.shop.domain;

import java.util.List;

public record ShopDefinition(String id, String nameKey, String refreshProfile, List<ShopOffer> offers) {
    public ShopDefinition {
        if (id == null || id.isBlank() || nameKey == null || nameKey.isBlank() || refreshProfile == null || refreshProfile.isBlank() || offers == null) throw new IllegalArgumentException("invalid shop definition");
        offers = List.copyOf(offers);
    }
}
