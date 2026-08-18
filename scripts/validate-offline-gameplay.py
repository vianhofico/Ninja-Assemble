#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
service_path = ROOT / "client-unity/Assets/Scripts/Game/Playable/OfflinePlayableGameService.cs"
sim_path = ROOT / "client-unity/Assets/Scripts/Game/Playable/OfflineBattleSimulator.cs"
save_path = ROOT / "client-unity/Assets/Scripts/Game/Playable/OfflineSaveData.cs"
errors = []

for path in (service_path, sim_path, save_path):
    if not path.exists(): errors.append(f"missing {path.relative_to(ROOT)}")

if not errors:
    service = service_path.read_text(encoding="utf-8")
    simulator = sim_path.read_text(encoding="utf-8")
    save = save_path.read_text(encoding="utf-8")

    required_service = [
        "OfflineBattleSimulator.Simulate",
        "PlayCampaignStageAsync",
        "SweepCampaignStageAsync",
        "PlayResourcePveAsync",
        "SummonAsync",
        "LevelUpAsync",
        "state.diamond -= PlayableGameStore.CompleteRosterSummonCost",
        "state.gold -= cost",
        "saves.Save(state)",
    ]
    for needle in required_service:
        if needle not in service: errors.append(f"offline service missing {needle!r}")

    for signature in (
        "PlayCampaignStageAsync(string playerId, string stageId) => Unsupported",
        "SummonAsync(string playerId, string requestId) => Unsupported",
        "LevelUpAsync(string playerId, string playerHeroId, string requestId) => Unsupported",
    ):
        if signature in service: errors.append(f"critical offline action still unsupported: {signature}")

    for needle in ("StableSeed", "new Random", 'outcome = "TEAM_A"', 'type = "DAMAGE"', 'type = "KO"'):
        if needle not in simulator: errors.append(f"offline deterministic battle missing {needle!r}")

    for needle in ("battleCount", "heroCoins", "ramen"):
        if needle not in save: errors.append(f"offline save missing persistent field {needle!r}")

if errors:
    print("offline-gameplay validation failed")
    for error in errors: print(" -", error)
    raise SystemExit(1)
print("offline-gameplay validation passed")
