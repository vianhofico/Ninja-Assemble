#!/usr/bin/env python3
"""Static integrity checks for M28 multi-wave campaign and inventory rewards."""
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def rows(path: str):
    with (ROOT / path).open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"M28_CONTRACT_INVALID {path} missing={missing}")


def main() -> int:
    enemies = rows("game-data/campaign/stage-enemies.csv")
    if len(enemies) != 75:
        raise SystemExit(f"expected 75 campaign enemy rows after boss wave expansion, got {len(enemies)}")
    for stage in ("c1-s4", "c2-s4", "c3-s4"):
        wave2 = [row for row in enemies if row["stage_id"] == stage and row["wave_index"] == "2"]
        if len(wave2) != 5:
            raise SystemExit(f"boss stage {stage} must have exactly five wave-2 enemies")

    items = rows("game-data/items/item-definitions.csv")
    if len(items) != 5 or len({row["item_id"] for row in items}) != 5:
        raise SystemExit("item catalog must contain five unique stack item definitions")
    if any(row["status"] != "DESIGN_BASELINE" for row in items):
        raise SystemExit("M28 item definitions must remain DESIGN_BASELINE")

    rewards = rows("game-data/campaign/stage-item-rewards.csv")
    if not rewards or {row["reward_scope"] for row in rewards} != {"FIRST", "REPEAT"}:
        raise SystemExit("campaign item rewards must cover FIRST and REPEAT scopes")
    item_ids = {row["item_id"] for row in items}
    if any(row["item_id"] not in item_ids or int(row["quantity"]) <= 0 for row in rewards):
        raise SystemExit("campaign item reward references invalid item/quantity")

    require(
        "server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java",
        "WAVE_RULES_VERSION", "SplittableRandom", "CampaignWaveResult", "List<CampaignWaveResult>", "itemRewards")
    require(
        "server/src/main/java/com/ninjaassemble/inventory/application/InventoryService.java",
        "inventory_stacks", "inventory_ledger", "for update", "idempotencyKey", "InventoryView")
    require(
        "server/src/main/java/com/ninjaassemble/campaign/application/CampaignRewardService.java",
        "inventory.mutate", "keyPrefix", 'return grant(playerId, stageId, reward, grantId, "campaign")',
        'keyPrefix + ":" + grantId + ":item:"')
    require(
        "client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
        "CampaignWaveDto", "InventoryViewDto", "itemRewards", "waveRulesVersion")
    require(
        "client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
        "GetInventoryAsync", "/inventory")
    require(
        "client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
        "RefreshInventoryAsync", "InventoryViewDto", "Task.WhenAll")
    require(
        "client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
        "WaitForReplayAsync", "CampaignWaveDto[] waves", "ScreenId.Inventory", "BuildInventory", "W{stage.waveCount}")

    print(f"M28_MULTIWAVE_INVENTORY_OK enemy_rows={len(enemies)} items={len(items)} reward_rows={len(rewards)} key_namespace=dynamic")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
