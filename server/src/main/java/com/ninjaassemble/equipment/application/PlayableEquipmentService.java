package com.ninjaassemble.equipment.application;

import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import com.ninjaassemble.play.application.ActionRequestService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayableEquipmentService {
    private static final String ACTION = "EQUIPMENT_ENHANCE";
    private static final Set<String> STARTER_IDS = Set.of(
            "academy-kunai", "academy-vest", "academy-headband", "academy-pouch", "academy-sandals", "academy-scroll");

    private final JdbcTemplate jdbc;
    private final PlayableEquipmentCatalogService catalog;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;
    private final ActionRequestService requests;

    public PlayableEquipmentService(JdbcTemplate jdbc, PlayableEquipmentCatalogService catalog, HeroOwnershipService ownership,
                                    WalletService wallet, ActionRequestService requests) {
        this.jdbc = jdbc; this.catalog = catalog; this.ownership = ownership; this.wallet = wallet; this.requests = requests;
    }

    @Transactional
    public List<OwnedEquipmentView> grantStarterSet(UUID playerId) {
        for (String definitionId : STARTER_IDS) {
            catalog.require(definitionId);
            Long count = jdbc.queryForObject("select count(*) from player_equipment where player_id = ? and equipment_definition_id = ?",
                    Long.class, playerId, definitionId);
            if (count == null || count == 0) {
                jdbc.update("""
                        insert into player_equipment(id, player_id, equipment_definition_id, enhance_level, refine_level, rolled_stats)
                        values (?, ?, ?, 0, 0, '{}'::jsonb)
                        """, UUID.randomUUID(), playerId, definitionId);
            }
        }
        return list(playerId);
    }

    @Transactional(readOnly = true)
    public List<OwnedEquipmentView> list(UUID playerId) {
        return jdbc.query("""
                select pe.id, pe.equipment_definition_id, ed.name_key, ed.slot, ed.rarity,
                       pe.enhance_level, pe.refine_level, ed.max_enhance_level,
                       pe.equipped_player_hero_id, pe.equipped_slot
                from player_equipment pe
                join equipment_definitions ed on ed.id = pe.equipment_definition_id
                where pe.player_id = ?
                order by ed.rarity desc, ed.slot, ed.id
                """, (rs, row) -> new OwnedEquipmentView(
                rs.getObject("id", UUID.class), rs.getString("equipment_definition_id"), rs.getString("name_key"),
                rs.getString("slot"), rs.getString("rarity"), rs.getInt("enhance_level"), rs.getInt("refine_level"),
                rs.getInt("max_enhance_level"), rs.getObject("equipped_player_hero_id", UUID.class), rs.getString("equipped_slot")), playerId);
    }

    @Transactional
    public OwnedEquipmentView equip(UUID playerId, UUID playerHeroId, UUID equipmentId) {
        ownership.requireOwned(playerId, playerHeroId);
        OwnedEquipmentView equipment = requireOwned(playerId, equipmentId);
        jdbc.update("update player_equipment set equipped_player_hero_id = null, equipped_slot = null where equipped_player_hero_id = ? and equipped_slot = ?",
                playerHeroId, equipment.slot());
        jdbc.update("update player_equipment set equipped_player_hero_id = ?, equipped_slot = ? where player_id = ? and id = ?",
                playerHeroId, equipment.slot(), playerId, equipmentId);
        return requireOwned(playerId, equipmentId);
    }

    @Transactional
    public EnhanceResult enhance(UUID playerId, UUID equipmentId, UUID requestId) {
        Optional<String> existing = requests.existing(playerId, requestId, ACTION);
        if (existing.isPresent()) return decode(playerId, existing.get());
        requests.reserve(playerId, requestId, ACTION);
        OwnedEquipmentView before = requireOwned(playerId, equipmentId);
        if (before.enhanceLevel() >= before.maxEnhanceLevel()) throw new IllegalStateException("equipment is at max enhance level");
        long goldCost = (before.enhanceLevel() + 1L) * 150L;
        wallet.mutate(playerId, Currency.GOLD, -goldCost, "EQUIPMENT_ENHANCE", equipmentId.toString(), "equipment:" + requestId + ":gold");
        jdbc.update("update player_equipment set enhance_level = enhance_level + 1 where player_id = ? and id = ?", playerId, equipmentId);
        EnhanceResult result = new EnhanceResult(requireOwned(playerId, equipmentId), goldCost, "equipment-enhance-experimental-v1");
        requests.complete(playerId, requestId, equipmentId + "\t" + goldCost);
        return result;
    }

    @Transactional(readOnly = true)
    public OwnedEquipmentView requireOwned(UUID playerId, UUID equipmentId) {
        return jdbc.query("""
                select pe.id, pe.equipment_definition_id, ed.name_key, ed.slot, ed.rarity,
                       pe.enhance_level, pe.refine_level, ed.max_enhance_level,
                       pe.equipped_player_hero_id, pe.equipped_slot
                from player_equipment pe
                join equipment_definitions ed on ed.id = pe.equipment_definition_id
                where pe.player_id = ? and pe.id = ?
                """, (rs, row) -> new OwnedEquipmentView(
                rs.getObject("id", UUID.class), rs.getString("equipment_definition_id"), rs.getString("name_key"),
                rs.getString("slot"), rs.getString("rarity"), rs.getInt("enhance_level"), rs.getInt("refine_level"),
                rs.getInt("max_enhance_level"), rs.getObject("equipped_player_hero_id", UUID.class), rs.getString("equipped_slot")),
                playerId, equipmentId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("owned equipment not found"));
    }

    private EnhanceResult decode(UUID playerId, String stored) {
        String[] p = stored.split("\t", -1);
        if (p.length != 2) throw new IllegalStateException("corrupt stored equipment enhance response");
        UUID id = UUID.fromString(p[0]);
        return new EnhanceResult(requireOwned(playerId, id), Long.parseLong(p[1]), "equipment-enhance-experimental-v1");
    }

    public record OwnedEquipmentView(UUID id, String definitionId, String nameKey, String slot, String rarity,
                                     int enhanceLevel, int refineLevel, int maxEnhanceLevel,
                                     UUID equippedPlayerHeroId, String equippedSlot) {}
    public record EnhanceResult(OwnedEquipmentView equipment, long goldCost, String profileVersion) {}
}
