#!/usr/bin/env python3
"""Static contract checks for M35 playable equipment."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"EQUIPMENT_CONTRACT_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/resources/db/migration/V7__inventory_equipment_summon_shop.sql",
            "equipment_definitions", "player_equipment", "equipped_player_hero_id", "equipped_slot")
    require("server/src/main/java/com/ninjaassemble/equipment/domain/EquipmentSlot.java",
            "WEAPON", "ARMOR", "HEAD", "ACCESSORY", "BOOTS", "SPECIAL")
    require("server/src/main/java/com/ninjaassemble/equipment/domain/EquipmentLoadout.java",
            "definition/instance mismatch", "slots.put(definition.slot(), instance)")
    require("server/src/main/java/com/ninjaassemble/equipment/application/EquipmentApplicationService.java",
            "equipment-design-baseline-v1", "equipment-enhance-design-v1", "equipment-combat-bonus-v1",
            "DESIGN_BASELINE", "ensureStarterGear", "EquipmentLoadout", "for update", "wallet_ledger",
            "EQUIPMENT_ENHANCE", "applyCombatBonus", "powerBonus", "Math.addExact")
    require("server/src/main/java/com/ninjaassemble/play/domain/ExperimentalCombatStatsResolver.java",
            "EquipmentApplicationService", "COMBAT_BONUS_VERSION", "equipment.applyCombatBonus", "playerHeroId")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/equipment"', '"/equipment/{equipmentId}/equip/{playerHeroId}"',
            '"/equipment/{equipmentId}/unequip"', '"/equipment/{equipmentId}/enhance"')
    require("client-unity/Assets/Scripts/Game/Equipment/EquipmentPlayableBridge.cs",
            "RuntimeInitializeOnLoadMethod", "ScreenId.Inventory", "EQUIP NEXT", "ENHANCE GEAR",
            "/equipment/", "/enhance", "requestId")
    require("server/src/test/java/com/ninjaassemble/equipment/domain/EquipmentLoadoutTest.java",
            "sameSlotIsReplacedDeterministically")
    print("PLAYABLE_EQUIPMENT_OK starter=6 slots=6 equip=authoritative enhance=idempotent combat=applied")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
