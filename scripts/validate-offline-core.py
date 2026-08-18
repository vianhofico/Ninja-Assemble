#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

required = {
    "client-unity/Assets/Scripts/Game/Playable/IPlayableGameService.cs": ["interface IPlayableGameService"],
    "client-unity/Assets/Scripts/Game/Playable/OnlinePlayableGameService.cs": ["OnlinePlayableGameService", "IPlayableGameService"],
    "client-unity/Assets/Scripts/Game/Playable/OfflinePlayableGameService.cs": ["OfflinePlayableGameService", "IPlayableGameService", "OfflineSaveRepository"],
    "client-unity/Assets/Scripts/Game/Playable/OfflineSaveData.cs": ["saveVersion = 1", "formationIds", "clearedStageIds"],
    "client-unity/Assets/Scripts/Game/Playable/OfflineSaveRepository.cs": ["Application.persistentDataPath", "ninjaassemble-offline-save.json", "BackupExisting"],
    "client-unity/Assets/Scripts/Game/Playable/OfflineSeedFactory.cs": ["Offline Ninja", "1000000", "10000"],
    "client-unity/Assets/Scripts/Game/Bootstrap/MobileGameBootstrap.cs": ["OfflinePlaytest = 0", "new OfflinePlayableGameService()", "StartupError"],
    "client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs": ["IPlayableGameService api", "PlayableGameStore(IPlayableGameService api)"],
}

errors = []
for rel, needles in required.items():
    path = ROOT / rel
    if not path.exists():
        errors.append(f"missing {rel}")
        continue
    text = path.read_text(encoding="utf-8")
    for needle in needles:
        if needle not in text:
            errors.append(f"{rel}: missing contract {needle!r}")

bootstrap = (ROOT / "client-unity/Assets/Scripts/Game/Bootstrap/MobileGameBootstrap.cs").read_text(encoding="utf-8")
if "runtimeMode = MobileRuntimeMode.OfflinePlaytest" not in bootstrap:
    errors.append("development bootstrap must default to OfflinePlaytest")
if "new GameApiClient(apiConfig)" in bootstrap:
    errors.append("bootstrap must use the online service adapter instead of directly constructing GameApiClient")

store = (ROOT / "client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs").read_text(encoding="utf-8")
if "private readonly GameApiClient" in store:
    errors.append("PlayableGameStore is still coupled directly to GameApiClient")

if errors:
    print("offline-core validation failed")
    for error in errors:
        print(" -", error)
    raise SystemExit(1)

print("offline-core validation passed")
