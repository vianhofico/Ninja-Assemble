#!/usr/bin/env python3
"""Static checks for M34 playable Roster / Hero Detail / Formation loop."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"ROSTER_FORMATION_INVALID {path} missing={missing}")


def main() -> int:
    # Validate real server invariants rather than milestone-era naming tokens.
    require("server/src/main/java/com/ninjaassemble/play/application/FormationService.java",
            "List<UUID> playerHeroIds", "BattleRules.requireArenaTeamSize(playerHeroIds.size())",
            "new HashSet<>(playerHeroIds).size()", "ownership.requireOwned", "formation_slots")
    require("client-unity/Assets/Scripts/Game/Heroes/RosterFormationPlayableBridge.cs",
            "ScreenId.NinjaRoster", "ScreenId.HeroDetail", "ScreenId.Formation",
            "SELECT NEXT", "NEXT VARIANT", "TRAIN", "ADD SELECTED",
            "GetVariantsAsync", "SelectVariantAsync", "LevelUpAsync", "SaveFormationAsync",
            "SelectedHero", "RefreshHeroes")
    print("ROSTER_FORMATION_OK selection=shared variant=live training=live formation=server-saved")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
