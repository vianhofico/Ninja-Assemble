package com.ninjaassemble.inventory.application;

import com.ninjaassemble.inventory.domain.InventoryStack;
import com.ninjaassemble.inventory.domain.ItemType;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public final class InventoryService {
    private final JdbcTemplate jdbc;
    private final ItemCatalogService catalog;
    private final Clock clock;

    public InventoryService(JdbcTemplate jdbc, ItemCatalogService catalog, Clock clock) {
        this.jdbc = jdbc;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<InventoryStack> list(UUID playerId) {
        return jdbc.query("""
                select item_definition_id, item_type, quantity
                from inventory_stacks
                where player_id = ? and quantity > 0
                order by item_type, item_definition_id
                """, (rs, rowNum) -> new InventoryStack(
                rs.getString("item_definition_id"),
                ItemType.valueOf(rs.getString("item_type")),
                rs.getLong("quantity")), playerId);
    }

    @Transactional
    public InventoryStack mutate(UUID playerId, String itemDefinitionId, long delta, String reason, String idempotencyKey) {
        if (playerId == null || itemDefinitionId == null || itemDefinitionId.isBlank()) throw new IllegalArgumentException("player/item required");
        if (delta == 0) throw new IllegalArgumentException("inventory delta must be non-zero");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("inventory reason required");
        ItemCatalogService.ItemDefinition definition = catalog.require(itemDefinitionId);

        jdbc.queryForObject("select id from players where id = ? for update", UUID.class, playerId);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            InventoryStack replay = jdbc.query("""
                    select s.item_definition_id, s.item_type, s.quantity
                    from inventory_ledger l
                    join inventory_stacks s on s.player_id = l.player_id and s.item_definition_id = l.item_definition_id
                    where l.player_id = ? and l.idempotency_key = ?
                    """, rs -> rs.next() ? new InventoryStack(
                    rs.getString("item_definition_id"), ItemType.valueOf(rs.getString("item_type")), rs.getLong("quantity")) : null,
                    playerId, idempotencyKey);
            if (replay != null) return replay;
        }

        InventoryStack before = jdbc.query("""
                select item_definition_id, item_type, quantity
                from inventory_stacks
                where player_id = ? and item_definition_id = ?
                for update
                """, rs -> rs.next() ? new InventoryStack(
                rs.getString("item_definition_id"), ItemType.valueOf(rs.getString("item_type")), rs.getLong("quantity")) : null,
                playerId, itemDefinitionId);

        if (before != null && before.type() != definition.type()) throw new IllegalStateException("inventory item type mismatch: " + itemDefinitionId);
        long balanceBefore = before == null ? 0 : before.quantity();
        InventoryStack after = new InventoryStack(itemDefinitionId, definition.type(), balanceBefore).mutate(delta);

        if (before == null) {
            jdbc.update("""
                    insert into inventory_stacks(player_id, item_definition_id, item_type, quantity)
                    values (?, ?, ?, ?)
                    """, playerId, itemDefinitionId, definition.type().name(), after.quantity());
        } else {
            jdbc.update("""
                    update inventory_stacks set quantity = ?
                    where player_id = ? and item_definition_id = ?
                    """, after.quantity(), playerId, itemDefinitionId);
        }

        jdbc.update("""
                insert into inventory_ledger(player_id, item_definition_id, delta, balance_before, balance_after, reason, idempotency_key, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, playerId, itemDefinitionId, delta, balanceBefore, after.quantity(), reason, idempotencyKey, clock.instant());
        return after;
    }

    public InventoryView view(UUID playerId) {
        List<InventoryItemView> items = list(playerId).stream().map(stack -> {
            ItemCatalogService.ItemDefinition definition = catalog.require(stack.itemDefinitionId());
            return new InventoryItemView(definition.id(), definition.type().name(), definition.nameEn(), definition.nameVi(), stack.quantity());
        }).toList();
        return new InventoryView(ItemCatalogService.VERSION, items);
    }

    public record InventoryView(String catalogVersion, List<InventoryItemView> items) {}
    public record InventoryItemView(String itemId, String itemType, String nameEn, String nameVi, long quantity) {}
}
