#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECIPE = ROOT / "game-data/ui/mobile-screen-recipes.csv"
EXPECTED = {
    "Home", "NinjaRoster", "HeroDetail", "Formation", "Adventure", "Battle", "Summon",
    "Arena", "ShadowArena", "Guild", "Shop", "Inventory", "Quest", "Events", "Mail", "Settings"
}
REQUIRED_FILES = [
    ROOT / "client-unity/Assets/Editor/MobileSceneBuilder.cs",
    ROOT / "client-unity/Assets/Scripts/Game/Bootstrap/MobileGameBootstrap.cs",
    ROOT / "client-unity/Assets/Scripts/Game/UI/MobileScreenRoot.cs",
    ROOT / "client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
    ROOT / "client-unity/Assets/Scripts/Game/UI/SceneNavButton.cs",
    ROOT / "client-unity/Assets/Scripts/Game/UI/MobileTheme.cs",
]


def main() -> int:
    missing_files = [str(path.relative_to(ROOT)) for path in REQUIRED_FILES if not path.exists()]
    if missing_files:
        print("MOBILE_SHELL_MISSING_FILES", ", ".join(missing_files))
        return 1

    with RECIPE.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    ids = [row["screen_id"].strip() for row in rows]
    if len(ids) != len(set(ids)):
        print("MOBILE_SHELL_DUPLICATE_SCREEN_ID")
        return 1
    if set(ids) != EXPECTED:
        print("MOBILE_SHELL_SCREEN_SET_MISMATCH", sorted(EXPECTED - set(ids)), sorted(set(ids) - EXPECTED))
        return 1
    if any(row["required_mobile_release"].strip().lower() != "true" for row in rows):
        print("MOBILE_SHELL_NON_REQUIRED_CORE_SCREEN")
        return 1

    builder = (ROOT / "client-unity/Assets/Editor/MobileSceneBuilder.cs").read_text(encoding="utf-8")
    for scene_id in EXPECTED:
        if f"ScreenId.{scene_id}" not in builder:
            print("MOBILE_SHELL_BUILDER_MISSING", scene_id)
            return 1

    print(f"MOBILE_SHELL_OK screens={len(rows)} bootstrap=1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
