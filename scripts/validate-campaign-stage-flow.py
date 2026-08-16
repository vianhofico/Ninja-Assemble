#!/usr/bin/env python3
"""Validate M27 data-driven campaign stage flow across server and Unity client."""
from __future__ import annotations

import csv
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STAGES = ROOT / "game-data/campaign/stages.csv"
ENEMIES = ROOT / "game-data/campaign/stage-enemies.csv"


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"CAMPAIGN_FLOW_INVALID {path} missing={missing}")


def main() -> int:
    with STAGES.open(encoding="utf-8", newline="") as handle:
        stages = list(csv.DictReader(handle))
    with ENEMIES.open(encoding="utf-8", newline="") as handle:
        enemies = list(csv.DictReader(handle))

    if len(stages) != 12:
        raise SystemExit(f"expected 12 campaign stages, got {len(stages)}")
    ids = [row["stage_id"] for row in stages]
    if len(set(ids)) != len(ids):
        raise SystemExit("duplicate campaign stage IDs")
    if ids[0] != "c1-s1" or ids[-1] != "c3-s4":
        raise SystemExit("campaign stage order/endpoints changed unexpectedly")
    if {row["difficulty"] for row in stages} != {"NORMAL", "ELITE", "HEROIC"}:
        raise SystemExit("campaign must cover NORMAL ELITE HEROIC")

    counts = Counter(row["stage_id"] for row in enemies)
    if len(enemies) != 60 or any(counts.get(stage_id) != 5 for stage_id in ids):
        raise SystemExit(f"expected exactly five enemies for each of 12 stages, counts={dict(counts)}")
    unknown = sorted(set(counts) - set(ids))
    if unknown:
        raise SystemExit(f"enemy rows reference unknown stages: {unknown}")

    require("server/src/main/java/com/ninjaassemble/campaign/application/CampaignStageCatalogService.java",
            "campaign-stage-catalog-v1", "HeroCatalogService", "new WaveDefinition", "new StageDefinition")
    require("server/src/main/java/com/ninjaassemble/campaign/application/CampaignProgressService.java",
            "campaign_stage_progress", "on conflict (player_id, stage_id) do nothing", "firstClear")
    require("server/src/main/java/com/ninjaassemble/campaign/application/CampaignStageFlowService.java",
            "CampaignGate.evaluate", "gate.missing()", "bestStars", "firstClearReward")
    require("server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java",
            "DEFAULT_STAGE_ID = \"c1-s1\"", "stageFlow.requirePlayable", "stage.energyCost()", "progress.recordClear",
            "stage.firstClearReward()", "stage.repeatReward()", "campaignCatalogVersion")
    battle_source = (ROOT / "server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java").read_text(encoding="utf-8")
    if "VerticalEnemyTeamService" in battle_source or "vertical-1" in battle_source:
        raise SystemExit("playable campaign battle must no longer depend on the vertical-1 hard-coded enemy/reward path")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/campaign/stages"', '"/campaign/stages/{stageId}/battle"')
    require("server/src/test/java/com/ninjaassemble/campaign/application/CampaignStageCatalogServiceTest.java",
            "catalogContainsTwelveOrderedPlayableStagesAcrossThreeChapters", "prerequisiteChainAndRewardsAreDataDriven")
    require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
            "GetCampaignStagesAsync", "PlayCampaignStageAsync")
    require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
            "CampaignStageListDto Campaign", "RecommendedStage", "BattleCampaignAsync", "RefreshCampaignAsync")
    require("client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
            "BuildAdventure", "store.RecommendedStage", "BattleCampaignAsync(stage.stageId)")

    print("CAMPAIGN_FLOW_OK stages=12 enemies=60 difficulties=3 progress=first-clear+stars client=connected")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
