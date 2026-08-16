#!/usr/bin/env python3
"""Static/data contract checks for M37 Frame Advance + Evolution."""
from pathlib import Path
import csv

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"HERO_PROGRESSION_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/resources/db/migration/V1__bootstrap.sql", "frame_tier", "frame_advance_step")
    require("server/src/main/resources/db/migration/V3__hero_progression.sql", "hero_variant_unlocks", "hero_progression_events")
    require("server/src/main/java/com/ninjaassemble/progression/application/FrameAdvancePolicy.java",
            '"GENIN", 1', '"CHUNIN", 2', '"JONIN", 3', '"KAGE", 4', '"KAGE", "SIX_PATH"',
            "requires a verified late-game profile")
    require("server/src/main/java/com/ninjaassemble/progression/application/FrameAdvanceApplicationService.java",
            "for update", "ActionRequestService", "FRAME_ADVANCE", "hero_progression_events", "request id was used for another hero")
    require("server/src/main/java/com/ninjaassemble/progression/application/EvolutionApplicationService.java",
            "for update", "ActionRequestService", "HERO_EVOLUTION", "hasVariant", "unlockVariant", "hero_progression_events",
            "request id was used for another evolution target")
    require("server/src/main/java/com/ninjaassemble/progression/api/HeroProgressionController.java",
            '"/evolution-paths/{characterId}"', '"/heroes/{playerHeroId}/frame-advance"', '"/heroes/{playerHeroId}/evolve"')
    require("client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs",
            "ScreenId.HeroDetail", "FRAME ADVANCE", "EVOLVE", "frame-advance", "/evolve", "selectedIndex", "RefreshHeroes")
    require("server/src/test/java/com/ninjaassemble/progression/application/FrameAdvancePolicyTest.java",
            "followsTheVerifiedEarlyFrameStepCounts", "refusesToInventSixPathToAwakeningRules")
    require("server/src/test/java/com/ninjaassemble/progression/application/EvolutionPathCatalogServiceTest.java",
            "flagshipChainsArePackagedAndOrderedByRequirements")

    paths = list(csv.DictReader((ROOT / "game-data/progression/playable-evolution-paths.csv").open(encoding="utf-8")))
    if len(paths) < 20:
        raise SystemExit(f"HERO_PROGRESSION_INVALID evolution paths={len(paths)} expected>=20")
    if any(row["status"] != "EXPANSION_PLAYABLE_PROFILE" for row in paths):
        raise SystemExit("HERO_PROGRESSION_INVALID evolution status must remain EXPANSION_PLAYABLE_PROFILE")

    variants = set()
    for filename in ("variant-census.csv", "variant-census-expanded.csv"):
        with (ROOT / "game-data/reference" / filename).open(encoding="utf-8") as handle:
            variants.update((row["character_id"], row["variant"]) for row in csv.DictReader(handle))
    missing = []
    for row in paths:
        if (row["character_id"], row["target_variant"]) not in variants:
            missing.append((row["character_id"], row["target_variant"]))
        prereq = row["prerequisite_variant"]
        if prereq != "BASE" and (row["character_id"], prereq) not in variants:
            missing.append((row["character_id"], prereq))
    if missing:
        raise SystemExit(f"HERO_PROGRESSION_INVALID unknown variants={missing}")

    print(f"HERO_PROGRESSION_OK frame=verified-early evolution_paths={len(paths)} variants=validated audit=yes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
