package com.ninjaassemble.inventory.domain;

public record InventoryStack(String itemDefinitionId, ItemType type, long quantity) {
    public InventoryStack {
        if (itemDefinitionId == null || itemDefinitionId.isBlank() || type == null || quantity < 0) throw new IllegalArgumentException("invalid inventory stack");
    }

    public InventoryStack mutate(long delta) {
        long next = Math.addExact(quantity, delta);
        if (next < 0) throw new IllegalStateException("insufficient item quantity");
        return new InventoryStack(itemDefinitionId, type, next);
    }
}
