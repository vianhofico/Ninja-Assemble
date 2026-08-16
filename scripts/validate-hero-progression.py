#!/usr/bin/env python3
"""Progression integrity after the one-Awakening cutover.

Frame Advance remains a separate rank track. Legacy linear Variant Evolution is no
longer a playable progression system; one-time Awakening is the only form upgrade.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"HERO_PROGRESSION_INVALID {path} missing={missing}")


def reject(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    present = [token for token in tokens if token in text]
    if present:
        raise SystemExit(f"HERO_PROGRESSION_LEGACY {path} present={present}")


def absent(path: str) -> None:
    if (ROOT / path).exists():
        raise SystemExit(f"HERO_PROGRESSION_LEGACY file still exists: {path}")


def main() -> int:
    require("server/src/main/resources/db/migration/V1__bootstrap.sql", "frame_tier", "frame_advance_step")
    require("server/src/main/java/com/ninjaassemble/progression/application/FrameAdvancePolicy.java",
            '"GENIN", 1', '"CHUNIN", 2', '"JONIN", 3', '"KAGE", 4', '"KAGE", "SIX_PATH"',
            "requires a verified late-game profile")
    require("server/src/main/java/com/ninjaassemble/progression/application/FrameAdvanceApplicationService.java",
            "for update", "ActionRequestService", "FRAME_ADVANCE", "hero_progression_events", "request id was used for another hero")
    require("server/src/main/java/com/ninjaassemble/progression/api/HeroProgressionController.java",
            '"/heroes/{playerHeroId}/frame-advance"')
    reject("server/src/main/java/com/ninjaassemble/progression/api/HeroProgressionController.java",
           "/evolution-paths", "/evolve", "targetVariant", "EvolutionApplicationService", "EvolutionPathCatalogService")
    require("client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs",
            "ScreenId.HeroDetail", "FRAME ADVANCE", "frame-advance", "SelectedHero", "RefreshHeroes")
    reject("client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs",
           "EVOLVE", "/evolve", "EvolutionPathDto", "targetVariant", "NextPath")
    require("client-unity/Assets/Scripts/Game/Heroes/RosterFormationPlayableBridge.cs",
            "AWAKEN", "GetAwakeningAsync", "AwakenAsync", "hero.awakened")
    reject("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
           '"/heroes/{playerHeroId}/variant"', "VariantRequest", "selectVariant(")
    require("server/src/test/java/com/ninjaassemble/progression/application/FrameAdvancePolicyTest.java",
            "followsTheVerifiedEarlyFrameStepCounts", "refusesToInventSixPathToAwakeningRules")

    absent("server/src/main/java/com/ninjaassemble/progression/application/EvolutionApplicationService.java")
    absent("server/src/main/java/com/ninjaassemble/progression/application/EvolutionPathCatalogService.java")
    absent("server/src/test/java/com/ninjaassemble/progression/application/EvolutionPathCatalogServiceTest.java")
    absent("game-data/progression/playable-evolution-paths.csv")
    absent("game-data/progression/evolution-paths.csv")

    print("HERO_PROGRESSION_OK frame=retained form_upgrade=one-time-awakening linear_evolution=removed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
