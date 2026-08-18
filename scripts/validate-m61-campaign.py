#!/usr/bin/env python3
"""M61 production Campaign audit and release gate."""
from __future__ import annotations

import argparse
import csv
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
STAGES = ROOT / "game-data/campaign/stages.csv"
ENEMIES = ROOT / "game-data/campaign/stage-enemies.csv"
ITEM_REWARDS = ROOT / "game-data/campaign/stage-item-rewards.csv"
CENSUS = ROOT / "game-data/campaign/release-census.csv"
EXPECTED_STAGES = 12
EXPECTED_DIFFICULTIES = {"NORMAL", "ELITE", "HEROIC"}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise ValueError(f"{path} missing {missing}")


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--enforce", action="store_true"); args = parser.parse_args()
    try:
        subprocess.run([sys.executable, str(ROOT / "scripts/validate-campaign-stage-flow.py")], cwd=ROOT, check=True)
        subprocess.run([sys.executable, str(ROOT / "scripts/validate-multiwave-inventory.py")], cwd=ROOT, check=True)

        stages = rows(STAGES); enemies = rows(ENEMIES); rewards = rows(ITEM_REWARDS); census = rows(CENSUS)
        if len(stages) != EXPECTED_STAGES: raise ValueError(f"expected {EXPECTED_STAGES} stages, got {len(stages)}")
        stage_ids = [r["stage_id"].strip() for r in stages]
        if len(stage_ids) != len(set(stage_ids)): raise ValueError("duplicate stage_id")
        if {r["difficulty"].strip() for r in stages} != EXPECTED_DIFFICULTIES: raise ValueError("difficulty census must be NORMAL/ELITE/HEROIC")
        if any(not r["name_en"].strip() or not r["name_vi"].strip() for r in stages): raise ValueError("every Campaign stage requires EN/VI names")
        if any(int(r["energy_cost"]) <= 0 for r in stages): raise ValueError("every stage requires positive Energy cost")
        if any(int(r["repeat_gold"]) <= 0 or int(r["repeat_player_exp"]) <= 0 for r in stages): raise ValueError("every stage requires positive repeat Gold/EXP")

        enemy_stages = {r["stage_id"].strip() for r in enemies}
        if enemy_stages != set(stage_ids): raise ValueError("every release stage must have enemy waves")
        reward_stages = {r["stage_id"].strip() for r in rewards}
        missing_reward_stages = sorted(set(stage_ids) - reward_stages)
        if missing_reward_stages: raise ValueError(f"release stages missing item reward rows: {missing_reward_stages}")

        census_by_id = {r["stage_id"].strip(): r for r in census}
        if len(census_by_id) != len(census): raise ValueError("duplicate release census stage")
        if set(census_by_id) != set(stage_ids): raise ValueError("release census must match stages.csv exactly")
        for stage in stages:
            row = census_by_id[stage["stage_id"]]
            if row["difficulty"].strip() != stage["difficulty"].strip(): raise ValueError(f"{stage['stage_id']}: census difficulty mismatch")
            evidence = row["evidence_ref"].strip()
            if not evidence or not (ROOT / evidence).is_file(): raise ValueError(f"{stage['stage_id']}: missing committed evidence_ref")
            if args.enforce and row["release_status"].strip() != "PRODUCTION_READY": raise ValueError(f"{stage['stage_id']}: production release status not ready")

        require("server/src/main/resources/db/migration/V13__campaign_sweep.sql", "campaign_sweeps", "request_id uuid primary key", "energy_after")
        require("server/src/main/java/com/ninjaassemble/campaign/application/CampaignSweepService.java",
                "pg_advisory_xact_lock", "previously cleared stage", "campaign-sweep", "stage.repeatReward()", "progress.recordClear", "withReplayed(true)")
        require("server/src/main/java/com/ninjaassemble/campaign/application/CampaignRewardService.java",
                "String keyPrefix", "keyPrefix + \":\" + grantId", "CAMPAIGN_STAGE_REWARD")
        require("server/src/main/java/com/ninjaassemble/campaign/api/CampaignSweepController.java",
                '"/{stageId}/sweep"', "requestId is required", "sweeps.sweep")
        require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs", "CampaignSweepDto", "CampaignSweepItemDto")
        require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs", "SweepCampaignStageAsync", '/campaign/stages/{Escape(stageId)}/sweep')
        require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs", "SweepableStage", "SweepCampaignAsync", "RefreshCampaignAsync", "RefreshInventoryAsync")

        ready = sum(1 for r in census if r["release_status"].strip() == "PRODUCTION_READY")
        print(f"M61_CAMPAIGN_OK stages={len(stages)} ready={ready} difficulties=3 multiwave=1 sweep=idempotent rewards=first+repeat client_contract=1")
        return 0
    except (ValueError, subprocess.CalledProcessError, KeyError) as error:
        print(f"M61_CAMPAIGN_INVALID {error}", file=sys.stderr); return 1


if __name__ == "__main__": raise SystemExit(main())
