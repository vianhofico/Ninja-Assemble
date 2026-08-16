package com.ninjaassemble.equipment.application;

import com.ninjaassemble.battle.sim.BattleUnitSeed;
import com.ninjaassemble.economy.application.WalletService;
import com.ninjaassemble.economy.domain.Currency;
import com.ninjaassemble.equipment.domain.EquipmentDefinition;
import com.ninjaassemble.equipment.domain.EquipmentInstance;
import com.ninjaassemble.equipment.domain.EquipmentLoadout;
import com.ninjaassemble.equipment.domain.EquipmentSlot;
import com.ninjaassemble.hero.ownership.HeroOwnershipService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentApplicationService {
    public static final String CATALOG_VERSION = "equipment-design-baseline-v1";
    public static final String ENHANCE_PROFILE_VERSION = "equipment-enhance-design-v1";
    public static final String COMBAT_BONUS_VERSION = "equipment-combat-bonus-v1";

    private static final List<EquipmentDefinition> CATALOG = List.of(
            definition("academy-kunai", "equipment.academy_kunai", EquipmentSlot.WEAPON, "COMMON", 10, null,
                    Map.of("PHYSICAL_ATTACK", 45L, "CHAKRA_ATTACK", 30L)),
            definition("shinobi-vest", "equipment.shinobi_vest", EquipmentSlot.ARMOR, "COMMON", 10, null,
                    Map.of("PHYSICAL_DEFENSE", 38L, "CHAKRA_DEFENSE", 38L)),
            definition("leaf-headband", "equipment.leaf_headband", EquipmentSlot.HEAD, "COMMON", 10, "leaf-starter",
                    Map.of("HP", 260L)),
            definition("mission-charm", "equipment.mission_charm", EquipmentSlot.ACCESSORY, "RARE", 12, "leaf-starter",
                    Map.of("PHYSICAL_ATTACK", 20L, "CHAKRA_ATTACK", 20L, "HP", 100L)),
            definition("shinobi-sandals", "equipment.shinobi_sandals", EquipmentSlot.BOOTS, "COMMON", 10, null,
                    Map.of("SPEED", 5L, "HP", 80L)),
            definition("chakra-scroll", "equipment.chakra_scroll", EquipmentSlot.SPECIAL, "RARE", 12, "leaf-starter",
                    Map.of("CHAKRA_ATTACK", 35L, "CHAKRA_DEFENSE", 20L))
    );
    private static final Map<String, EquipmentDefinition> BY_ID = catalogById();

    private final JdbcTemplate jdbc;
    private final HeroOwnershipService ownership;
    private final WalletService wallet;

    public EquipmentApplicationService(JdbcTemplate jdbc, HeroOwnershipService ownership, WalletService wallet) {
        this.jdbc = jdbc;
        this.ownership = ownership;
        this.wallet = wallet;
    }

    @Transactional
    public EquipmentView view(UUID playerId) {
        ownership.list(playerId);
        ensureDefinitions();
        ensureStarterGear(playerId);
        return new EquipmentView(CATALOG_VERSION, ENHANCE_PROFILE_VERSION, COMBAT_BONUS_VERSION,
                listOwned(playerId), aggregateEquippedBonusForPlayer(playerId));
    }

    @Transactional
    public EquipmentView equip(UUID playerId, UUID equipmentId, UUID playerHeroId) {
        ownership.requireOwned(playerId, playerHeroId);
        ensureDefinitions();
        EquipmentRow row = requireEquipment(playerId, equipmentId, true);
        EquipmentDefinition definition = requireDefinition(row.definitionId());

        EquipmentLoadout loadout = new EquipmentLoadout();
        loadout.equip(definition, row.instance());

        jdbc.update("""
                update player_equipment
                set equipped_player_hero_id = null, equipped_slot = null
                where player_id = ? and equipped_player_hero_id = ? and equipped_slot = ? and id <> ?
                """, playerId, playerHeroId, definition.slot().name(), equipmentId);
        jdbc.update("""
                update player_equipment
                set equipped_player_hero_id = ?, equipped_slot = ?
                where id = ? and player_id = ?
                """, playerHeroId, definition.slot().name(), equipmentId, playerId);
        return view(playerId);
    }

    @Transactional
    public EquipmentView unequip(UUID playerId, UUID equipmentId) {
        requireEquipment(playerId, equipmentId, true);
        jdbc.update("update player_equipment set equipped_player_hero_id = null, equipped_slot = null where id = ? and player_id = ?",
                equipmentId, playerId);
        return view(playerId);
    }

    @Transactional
    public EnhanceResult enhance(UUID playerId, UUID equipmentId, UUID requestId) {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        String key = "equipment-enhance:" + requestId;

        // Lock first, then inspect the ledger. Two concurrent retries with the same requestId
        // therefore serialize on the equipment row and cannot advance two levels.
        EquipmentRow row = requireEquipment(playerId, equipmentId, true);
        Long seen = jdbc.queryForObject("select count(*) from wallet_ledger where player_id = ? and idempotency_key = ?", Long.class, playerId, key);
        if (seen != null && seen > 0) {
            return new EnhanceResult(replayView(row), 0, wallet.getBalance(playerId, Currency.GOLD), true, ENHANCE_PROFILE_VERSION);
        }

        EquipmentDefinition definition = requireDefinition(row.definitionId());
        if (row.enhanceLevel() >= definition.maxEnhanceLevel()) throw new IllegalStateException("equipment is already at max enhance level");
        int targetLevel = row.enhanceLevel() + 1;
        long cost = enhanceCost(targetLevel);
        long goldAfter = wallet.mutate(playerId, Currency.GOLD, -cost, "EQUIPMENT_ENHANCE", equipmentId.toString(), key);
        jdbc.update("update player_equipment set enhance_level = ? where id = ? and player_id = ?", targetLevel, equipmentId, playerId);
        EquipmentRow updated = requireEquipment(playerId, equipmentId, false);
        return new EnhanceResult(replayView(updated), cost, goldAfter, false, ENHANCE_PROFILE_VERSION);
    }

    @Transactional(readOnly = true)
    public BattleUnitSeed applyCombatBonus(UUID playerHeroId, BattleUnitSeed seed) {
        if (playerHeroId == null || seed == null) throw new IllegalArgumentException("hero and battle seed are required");
        Map<String, Long> bonus = aggregateBonus(playerHeroId);
        return new BattleUnitSeed(
                seed.id(), seed.side(), seed.slot(), add(seed.maxHp(), bonus.get("HP")),
                add(seed.physicalAttack(), bonus.get("PHYSICAL_ATTACK")),
                add(seed.chakraAttack(), bonus.get("CHAKRA_ATTACK")),
                add(seed.physicalDefense(), bonus.get("PHYSICAL_DEFENSE")),
                add(seed.chakraDefense(), bonus.get("CHAKRA_DEFENSE")),
                Math.toIntExact(add(seed.speed(), bonus.get("SPEED"))),
                seed.physicalCritBps(), seed.chakraCritBps(), seed.primaryChannel(), seed.abilities(), seed.passives());
    }

    @Transactional(readOnly = true)
    public long powerBonus(UUID playerHeroId) {
        Map<String, Long> bonus = aggregateBonus(playerHeroId);
        return bonus.getOrDefault("HP", 0L)
                + 8L * (bonus.getOrDefault("PHYSICAL_ATTACK", 0L) + bonus.getOrDefault("CHAKRA_ATTACK", 0L))
                + 5L * (bonus.getOrDefault("PHYSICAL_DEFENSE", 0L) + bonus.getOrDefault("CHAKRA_DEFENSE", 0L))
                + 25L * bonus.getOrDefault("SPEED", 0L);
    }

    private void ensureDefinitions() {
        for (EquipmentDefinition definition : CATALOG) {
            jdbc.update("""
                    insert into equipment_definitions(id, name_key, slot, rarity, max_enhance_level, set_id, content, parity_status)
                    values (?, ?, ?, ?, ?, ?, jsonb_build_object('profile', ?, 'baseStats', ?), 'DESIGN_BASELINE')
                    on conflict (id) do update set name_key = excluded.name_key, slot = excluded.slot, rarity = excluded.rarity,
                        max_enhance_level = excluded.max_enhance_level, set_id = excluded.set_id, content = excluded.content,
                        parity_status = excluded.parity_status
                    """, definition.id(), definition.nameKey(), definition.slot().name(), definition.rarity(),
                    definition.maxEnhanceLevel(), definition.setId(), CATALOG_VERSION, statsString(definition.baseStats()));
        }
    }

    private void ensureStarterGear(UUID playerId) {
        Long count = jdbc.queryForObject("select count(*) from player_equipment where player_id = ?", Long.class, playerId);
        if (count != null && count > 0) return;
        for (EquipmentDefinition definition : CATALOG) {
            jdbc.update("""
                    insert into player_equipment(id, player_id, equipment_definition_id, enhance_level, refine_level, rolled_stats)
                    values (?, ?, ?, 0, 0, '{}'::jsonb)
                    """, UUID.randomUUID(), playerId, definition.id());
        }
    }

    private List<EquipmentItemView> listOwned(UUID playerId) {
        return jdbc.query("""
                select id, equipment_definition_id, enhance_level, refine_level, equipped_player_hero_id, equipped_slot
                from player_equipment where player_id = ?
                order by equipment_definition_id, id
                """, (rs, index) -> {
            EquipmentDefinition definition = requireDefinition(rs.getString("equipment_definition_id"));
            return new EquipmentItemView(
                    rs.getObject("id", UUID.class), definition.id(), definition.nameKey(), definition.slot().name(), definition.rarity(),
                    rs.getInt("enhance_level"), definition.maxEnhanceLevel(), rs.getInt("refine_level"),
                    rs.getObject("equipped_player_hero_id", UUID.class), rs.getString("equipped_slot"),
                    scaledStats(definition.baseStats(), rs.getInt("enhance_level")));
        }, playerId);
    }

    private EquipmentRow requireEquipment(UUID playerId, UUID equipmentId, boolean lock) {
        String sql = "select id, equipment_definition_id, enhance_level, refine_level, equipped_player_hero_id, equipped_slot " +
                "from player_equipment where player_id = ? and id = ?" + (lock ? " for update" : "");
        return jdbc.query(sql, (rs, index) -> new EquipmentRow(
                rs.getObject("id", UUID.class), rs.getString("equipment_definition_id"), rs.getInt("enhance_level"),
                rs.getInt("refine_level"), rs.getObject("equipped_player_hero_id", UUID.class), rs.getString("equipped_slot")),
                playerId, equipmentId).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("equipment not found"));
    }

    private EquipmentItemView replayView(EquipmentRow row) {
        EquipmentDefinition definition = requireDefinition(row.definitionId());
        return new EquipmentItemView(row.id(), definition.id(), definition.nameKey(), definition.slot().name(), definition.rarity(),
                row.enhanceLevel(), definition.maxEnhanceLevel(), row.refineLevel(), row.equippedHeroId(), row.equippedSlot(),
                scaledStats(definition.baseStats(), row.enhanceLevel()));
    }

    private Map<String, Long> aggregateBonus(UUID playerHeroId) {
        Map<String, Long> result = new LinkedHashMap<>();
        List<EquipmentRow> rows = jdbc.query("""
                select id, equipment_definition_id, enhance_level, refine_level, equipped_player_hero_id, equipped_slot
                from player_equipment where equipped_player_hero_id = ?
                """, (rs, index) -> new EquipmentRow(
                rs.getObject("id", UUID.class), rs.getString("equipment_definition_id"), rs.getInt("enhance_level"),
                rs.getInt("refine_level"), rs.getObject("equipped_player_hero_id", UUID.class), rs.getString("equipped_slot")), playerHeroId);
        for (EquipmentRow row : rows) {
            EquipmentDefinition definition = BY_ID.get(row.definitionId());
            if (definition == null) continue;
            scaledStats(definition.baseStats(), row.enhanceLevel()).forEach((key, value) -> result.merge(key, value, Math::addExact));
        }
        return Map.copyOf(result);
    }

    private Map<String, Long> aggregateEquippedBonusForPlayer(UUID playerId) {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (UUID heroId : jdbc.query("select distinct equipped_player_hero_id from player_equipment where player_id = ? and equipped_player_hero_id is not null",
                (rs, row) -> rs.getObject(1, UUID.class), playerId)) {
            aggregateBonus(heroId).forEach((key, value) -> totals.merge(key, value, Math::addExact));
        }
        return Map.copyOf(totals);
    }

    private static Map<String, Long> scaledStats(Map<String, Long> base, int enhanceLevel) {
        Map<String, Long> result = new LinkedHashMap<>();
        long multiplierBps = 10_000L + enhanceLevel * 750L;
        base.forEach((key, value) -> result.put(key, Math.max(0L, value * multiplierBps / 10_000L)));
        return Map.copyOf(result);
    }

    private static long enhanceCost(int targetLevel) {
        return 300L + 250L * targetLevel * targetLevel;
    }

    private static EquipmentDefinition requireDefinition(String id) {
        EquipmentDefinition definition = BY_ID.get(id);
        if (definition == null) throw new IllegalArgumentException("unknown equipment definition: " + id);
        return definition;
    }

    private static EquipmentDefinition definition(String id, String nameKey, EquipmentSlot slot, String rarity,
                                                  int maxEnhanceLevel, String setId, Map<String, Long> stats) {
        return new EquipmentDefinition(id, nameKey, slot, rarity, maxEnhanceLevel, setId, stats);
    }

    private static Map<String, EquipmentDefinition> catalogById() {
        Map<String, EquipmentDefinition> map = new LinkedHashMap<>();
        for (EquipmentDefinition definition : CATALOG) map.put(definition.id(), definition);
        return Map.copyOf(map);
    }

    private static String statsString(Map<String, Long> stats) {
        return String.join("|", stats.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
    }

    private static long add(long base, Long bonus) {
        return Math.addExact(base, bonus == null ? 0L : bonus);
    }

    private record EquipmentRow(UUID id, String definitionId, int enhanceLevel, int refineLevel,
                                UUID equippedHeroId, String equippedSlot) {
        EquipmentInstance instance() { return new EquipmentInstance(id, definitionId, enhanceLevel, refineLevel, Map.of()); }
    }

    public record EquipmentItemView(UUID equipmentId, String definitionId, String nameKey, String slot, String rarity,
                                    int enhanceLevel, int maxEnhanceLevel, int refineLevel,
                                    UUID equippedPlayerHeroId, String equippedSlot, Map<String, Long> stats) {}
    public record EquipmentView(String catalogVersion, String enhanceProfileVersion, String combatBonusVersion,
                                List<EquipmentItemView> equipment, Map<String, Long> equippedBonusTotals) {}
    public record EnhanceResult(EquipmentItemView equipment, long goldCost, long goldAfter, boolean replayed,
                                String enhanceProfileVersion) {}
}
