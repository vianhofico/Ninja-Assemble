#!/usr/bin/env python3
"""Validate playable-baseline completeness and optional production-release evidence."""
from __future__ import annotations
import argparse
import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COMPONENT_FIELDS = (
    "portrait_status", "icon_status", "chibi_prefab_status", "animation_status",
    "vfx_status", "sfx_status", "regression_capture_status", "review_status",
)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8-sig")


def require(path: str, *tokens: str) -> list[str]:
    text = read(path)
    return [f"{path}: missing {token}" for token in tokens if token not in text]


def reject(path: str, *tokens: str) -> list[str]:
    text = read(path)
    return [f"{path}: legacy token present {token}" for token in tokens if token in text]


def csv_rows(path: str) -> list[dict[str, str]]:
    with (ROOT / path).open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true", help="enforce production art/device evidence")
    args = parser.parse_args()
    errors: list[str] = []

    screen_text = read("client-unity/Assets/Scripts/Game/UI/ScreenId.cs")
    match = re.search(r"public enum ScreenId\s*\{([^}]*)\}", screen_text, re.S)
    if not match:
        raise SystemExit("RELEASE_READINESS_INVALID cannot parse ScreenId")
    screens = [item.strip().strip(",") for item in match.group(1).splitlines() if item.strip()]
    expected = ["Home", "NinjaRoster", "HeroDetail", "Formation", "Adventure", "Battle", "Summon", "Arena",
                "ShadowArena", "Guild", "Shop", "Inventory", "Quest", "Events", "Mail", "Settings",
                "ResourcePve", "Progression"]
    if screens != expected:
        errors.append(f"ScreenId coverage changed: actual={screens}")

    errors += require("client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
                      "ScreenId.Home", "ScreenId.Adventure", "ScreenId.Battle", "ScreenId.Summon", "ScreenId.Arena",
                      "ScreenId.ShadowArena", "ScreenId.Shop", "ScreenId.Inventory", "ScreenId.Quest", "ScreenId.Mail")
    errors += require("client-unity/Assets/Scripts/Game/Heroes/RosterFormationPlayableBridge.cs",
                      "ScreenId.NinjaRoster", "ScreenId.HeroDetail", "ScreenId.Formation", "SaveFormationAsync",
                      "AWAKEN", "GetAwakeningAsync", "AwakenAsync")
    errors += require("client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs",
                      "FRAME ADVANCE", "frame-advance")
    errors += reject("client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs",
                     "EVOLVE", "/evolve", "EvolutionPathDto", "targetVariant")
    errors += require("client-unity/Assets/Scripts/Game/Guild/GuildPlayableBridge.cs", "ScreenId.Guild", "HIT GUILD BOSS")
    errors += require("client-unity/Assets/Scripts/Game/Equipment/EquipmentPlayableBridge.cs", "ScreenId.Inventory", "ENHANCE GEAR")
    errors += require("client-unity/Assets/Scripts/Game/Events/WeeklyEventPlayableBridge.cs", "ScreenId.Events", "/events")
    errors += require("client-unity/Assets/Scripts/Game/Settings/SettingsPlayableBridge.cs", "ScreenId.Settings", "PlayerPrefs.Save")
    errors += require("client-unity/Assets/Scripts/Game/Localization/BilingualRuntimeLocalizationBridge.cs",
                      "Localization/strings", "ToVietnamese", "ToEnglish")

    server_contracts = {
        "Campaign": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/campaign/stages/{stageId}/battle"'),
        "Inventory": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/inventory"'),
        "Equipment": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/equipment/{equipmentId}/enhance"'),
        "Arena": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/arena/{opponentPlayerId}/battle"'),
        "ShadowArena": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/shadow-arena/{opponentPlayerId}/battle"'),
        "Guild": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/guild/boss/hit"'),
        "Shop": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/shop/{shopId}/{offerId}/purchase"'),
        "Quest": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/quests/{questId}/claim"'),
        "Events": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/events/{objectiveId}/claim"'),
        "Mail": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/mail/{mailId}/claim"'),
        "Summon": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/summon"'),
        "LevelUp": ("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java", '"/heroes/{playerHeroId}/level-up"'),
        "FrameAdvance": ("server/src/main/java/com/ninjaassemble/progression/api/HeroProgressionController.java", '"/heroes/{playerHeroId}/frame-advance"'),
        "Awakening": ("server/src/main/java/com/ninjaassemble/hero/awakening/HeroAwakeningController.java", '"/api/v1/play/{playerId}/heroes/{playerHeroId}/awakening"'),
    }
    for name, (path, token) in server_contracts.items():
        if token not in read(path): errors.append(f"server contract missing: {name} {token}")
    errors += reject("server/src/main/java/com/ninjaassemble/progression/api/HeroProgressionController.java",
                     "/evolve", "/evolution-paths", "targetVariant")
    errors += reject("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
                     '"/heroes/{playerHeroId}/variant"', "VariantRequest")

    required_workflows = [
        "server-core.yml", "content-integrity.yml", "guild-integrity.yml", "roster-formation-integrity.yml",
        "equipment-integrity.yml", "events-settings-integrity.yml", "hero-progression-integrity.yml",
        "runtime-localization-integrity.yml",
    ]
    for workflow in required_workflows:
        if not (ROOT / ".github/workflows" / workflow).exists(): errors.append(f"missing workflow: {workflow}")

    census: set[tuple[str, str]] = set()
    for path in sorted((ROOT / "game-data/reference").glob("variant-census*.csv")):
        with path.open(encoding="utf-8-sig", newline="") as handle:
            census.update((row["character_id"].strip(), row["variant"].strip()) for row in csv.DictReader(handle))
    components = csv_rows("art/manifests/hero-art-component-status.csv")
    complete = sum(all(row.get(field, "").strip().upper() == "READY" for field in COMPONENT_FIELDS) for row in components)

    evidence = csv_rows("game-data/release/mobile-device-evidence.csv")
    passing = [row for row in evidence if row.get("smoke_pass", "").strip().lower() in {"true", "1", "yes"}
               and row.get("performance_pass", "").strip().lower() in {"true", "1", "yes"}]
    devices = {row.get("device_model", "").strip() for row in passing if row.get("device_model", "").strip()}
    classes = {row.get("device_class", "").strip().upper() for row in passing if row.get("device_class", "").strip()}

    if args.release:
        component_keys = {(row["character_id"].strip(), row["variant"].strip()) for row in components}
        if component_keys != census:
            errors.append(f"production art coverage incomplete: tracked={len(component_keys)} census={len(census)}")
        if complete != len(census):
            errors.append(f"production art packages incomplete: complete={complete} census={len(census)}")
        if len(passing) < 2 or len(devices) < 2 or len(classes) < 2:
            errors.append(f"mobile evidence incomplete: passing={len(passing)} devices={len(devices)} classes={len(classes)}")

    if errors:
        print("RELEASE_READINESS_INVALID")
        for error in errors: print(" -", error)
        return 1

    mode = "production-release" if args.release else "playable-baseline"
    print(f"RELEASE_READINESS_OK mode={mode} screens={len(screens)} server_contracts={len(server_contracts)} workflows={len(required_workflows)}")
    print(f"ART_STATUS census={len(census)} tracked={len(components)} complete={complete}")
    print(f"DEVICE_STATUS rows={len(evidence)} passing={len(passing)} devices={len(devices)} classes={len(classes)}")
    if not args.release and (complete < len(census) or len(passing) < 2):
        print("PRODUCTION_RELEASE_BLOCKED art_or_device_evidence_incomplete=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
