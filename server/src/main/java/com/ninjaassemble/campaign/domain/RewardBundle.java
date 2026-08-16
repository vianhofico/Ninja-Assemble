package com.ninjaassemble.campaign.domain;

import java.util.Map;

public record RewardBundle(long playerExp, Map<String, Long> currencies, Map<String, Long> items) {
    public RewardBundle {
        if (playerExp < 0) throw new IllegalArgumentException("negative exp");
        currencies = currencies == null ? Map.of() : Map.copyOf(currencies);
        items = items == null ? Map.of() : Map.copyOf(items);
        if (currencies.values().stream().anyMatch(v -> v == null || v < 0) || items.values().stream().anyMatch(v -> v == null || v < 0)) {
            throw new IllegalArgumentException("negative reward amount");
        }
    }
}
