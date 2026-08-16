#!/usr/bin/env python3
"""Static checks for M30 playable shop."""
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"SHOP_CONTRACT_INVALID {path} missing={missing}")


def main() -> int:
    with (ROOT / "game-data/shop/shop-offers.csv").open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if len(rows) != 8 or {row["shop_id"] for row in rows} != {"general", "arena", "hero"}:
        raise SystemExit("shop catalog must contain 8 offers across general/arena/hero")
    if any(row["status"] != "DESIGN_BASELINE" or row["refresh_profile"] != "DAILY_05" for row in rows):
        raise SystemExit("shop offers must remain DESIGN_BASELINE DAILY_05")
    if len({row["offer_id"] for row in rows}) != len(rows):
        raise SystemExit("duplicate shop offer IDs")

    require("server/src/main/java/com/ninjaassemble/shop/application/ShopCatalogService.java",
            "shop-catalog-design-v1", "ItemCatalogService", "Currency.valueOf", "DESIGN_BASELINE")
    require("server/src/main/java/com/ninjaassemble/shop/application/ShopApplicationService.java",
            "ShopPurchaseGate.evaluate", "shop_purchase_state", "wallet_ledger", "for update", "inventory.mutate",
            "game.clock.zone", "game.clock.reset-hour", "requestId")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/shop"', '"/shop/{shopId}/{offerId}/purchase"')
    require("server/src/test/java/com/ninjaassemble/shop/application/ShopCatalogServiceTest.java",
            "designCatalogContainsThreeValidatedDailyShopsAndEightOffers")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
            "ShopOfferDto", "ShopViewDto", "ShopPurchaseResultDto")
    require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs", "GetShopAsync", "PurchaseShopAsync")
    require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
            "ShopViewDto Shop", "RefreshShopAsync", "PurchaseShopAsync")
    require("client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
            "ScreenId.Shop", "TryFirstPurchasable", "BuildShop", "BUY NEXT OFFER")
    print("PLAYABLE_SHOP_OK shops=3 offers=8 reset=DAILY_05 idempotency=wallet-ledger inventory=authoritative")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
