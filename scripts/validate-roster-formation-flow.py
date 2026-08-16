#!/usr/bin/env python3
"""Static checks for the playable Roster / Hero Detail / Formation loop after M46 Awakening cutover."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"ROSTER_FORMATION_INVALID {path} missing={missing}")


def reject(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    present = [token for token in tokens if token in text]
    if present:
        raise SystemExit(f"ROSTER_FORMATION_LEGACY_UI {path} present={present}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/play/application/FormationService.java",
            "List<UUID> playerHeroIds", "BattleRules.requireArenaTeamSize(playerHeroIds.size())",
            "new HashSet<>(playerHeroIds).size()", "ownership.requireOwned", "formation_slots")
    bridge = "client-unity/Assets/Scripts/Game/Heroes/RosterFormationPlayableBridge.cs"
    require(bridge,
            "ScreenId.NinjaRoster", "ScreenId.HeroDetail", "ScreenId.Formation",
            "SELECT NEXT", "AWAKEN", "TRAIN", "ADD SELECTED",
            "GetAwakeningAsync", "AwakenAsync", "LevelUpAsync", "SaveFormationAsync",
            "hero.heroId", "hero.awakened", "SelectedHero", "RefreshHeroes")
    reject(bridge, "NEXT VARIANT", "GetVariantsAsync", "SelectNextVariant")
    print("ROSTER_FORMATION_OK selection=shared awakening=one-time training=live formation=server-saved")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
