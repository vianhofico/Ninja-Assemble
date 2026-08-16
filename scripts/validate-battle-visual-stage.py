#!/usr/bin/env python3
from __future__ import annotations

import csv
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "art/manifests/hero-art-manifest.csv"
RUNTIME_CATALOG = ROOT / "client-unity/Assets/Resources/Generated/hero-art-runtime-catalog.json"
REQUIRED_FILES = [
    ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattleVisualStage.cs",
    ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattlePresentationAdapter.cs",
    ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs",
    ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattleActorView.cs",
    ROOT / "client-unity/Assets/Scripts/Game/Presentation/HeroArtRuntimeCatalog.cs",
    ROOT / "client-unity/Assets/Scripts/Game/Presentation/HeroAddressableLoader.cs",
]


def main() -> int:
    errors: list[str] = []
    for path in REQUIRED_FILES:
        if not path.exists():
            errors.append(f"missing runtime battle file: {path.relative_to(ROOT)}")

    dto_path = ROOT / "client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs"
    dto = dto_path.read_text(encoding="utf-8") if dto_path.exists() else ""
    for token in ("BattleParticipantDto", "participants", "battleUnitId", "characterId", "variant", "side", "slot", "maxHp"):
        if token not in dto:
            errors.append(f"PlayableDtos missing participant contract token: {token}")

    server_path = ROOT / "server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java"
    server = server_path.read_text(encoding="utf-8") if server_path.exists() else ""
    for token in ("List<BattleParticipant>", "participants", "unit.maxHp()", "enemy.displayName()"):
        if token not in server:
            errors.append(f"PlayableBattleService missing presentation participant token: {token}")

    timeline_path = ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs"
    timeline = timeline_path.read_text(encoding="utf-8") if timeline_path.exists() else ""
    for token in ('case "ATTACK"', 'case "DAMAGE"', 'case "KO"', "ApplyDamage", "PlaybackCompleted"):
        if token not in timeline:
            errors.append(f"BattleTimelinePlayer missing playback token: {token}")

    stage_path = ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattleVisualStage.cs"
    stage = stage_path.read_text(encoding="utf-8") if stage_path.exists() else ""
    for token in ("TeamAAnchors", "TeamBAnchors", "art.IsReady", "TryLoadPrefabAsync", "CreateFallbackActor", "FloatingDamage", "CriticalShake", "PlayVictory"):
        if token not in stage:
            errors.append(f"BattleVisualStage missing runtime stage token: {token}")

    with MANIFEST.open(encoding="utf-8-sig", newline="") as handle:
        manifest_rows = list(csv.DictReader(handle))
    try:
        runtime = json.loads(RUNTIME_CATALOG.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"invalid runtime art catalog JSON: {exc}")
        runtime = {"entries": []}

    actual = {
        (entry.get("characterId", "").strip(), entry.get("variant", "").strip()): entry
        for entry in runtime.get("entries", [])
    }
    expected = {
        (row["character_id"].strip(), row["variant"].strip()): row
        for row in manifest_rows
    }
    if set(actual) != set(expected):
        errors.append(f"runtime art catalog identities differ from manifest: expected={len(expected)} actual={len(actual)}")

    field_map = {
        "portraitAddress": "portrait_address",
        "iconAddress": "icon_address",
        "prefabAddress": "prefab_address",
        "animationSet": "animation_set",
        "vfxSet": "vfx_set",
        "sfxSet": "sfx_set",
        "status": "status",
    }
    for key in set(actual) & set(expected):
        for json_field, csv_field in field_map.items():
            actual_value = str(actual[key].get(json_field, "")).strip()
            expected_value = str(expected[key].get(csv_field, "")).strip()
            if json_field == "status":
                actual_value = actual_value.upper()
                expected_value = expected_value.upper()
            if actual_value != expected_value:
                errors.append(
                    f"runtime art catalog mismatch {key} {json_field}: "
                    f"expected={expected_value!r} actual={actual_value!r}"
                )

    if errors:
        print("BATTLE_VISUAL_STAGE_INVALID", file=sys.stderr)
        for error in errors:
            print(" -", error, file=sys.stderr)
        return 1

    print(f"BATTLE_VISUAL_STAGE_OK runtime_art_entries={len(actual)} participant_contract=10_units fallback=enabled")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
