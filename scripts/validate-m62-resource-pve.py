#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
from pathlib import Path
import sys

ROOT=Path(__file__).resolve().parents[1]
EXPECTED={"naruto-trial","forest-hunt","ninja-test","gold-challenge","daily-food","resource-raid","battle-relief","obito-ultimate-trial","tailed-beast-conquer"}
LEGACY={"LAND_OF_PAIN","NINJA_QUEST","CRUSADE","NINJA_TRIAL_AUTHENTIC_WATERFALL","NINJA_TRIAL_GAMA_TEMPLE","NINJA_TRIAL_PATH_OF_KUNOICHI"}

def rows(path):
    with (ROOT/path).open(encoding="utf-8-sig",newline="") as h:return list(csv.DictReader(h))
def require(path,*tokens):
    text=(ROOT/path).read_text(encoding="utf-8"); missing=[t for t in tokens if t not in text]
    if missing: raise ValueError(f"{path} missing {missing}")

def main():
    p=argparse.ArgumentParser();p.add_argument("--enforce",action="store_true");a=p.parse_args()
    try:
        modes=rows("game-data/pve/resource-modes.csv"); enemies=rows("game-data/pve/resource-mode-enemies.csv")
        ids=[r["mode_id"].strip() for r in modes]
        if set(ids)!=EXPECTED or len(ids)!=9: raise ValueError(f"release resource PvE census mismatch: {ids}")
        if len(ids)!=len(set(ids)): raise ValueError("duplicate resource PvE mode")
        for r in modes:
            if not r["name_en"].strip() or not r["name_vi"].strip(): raise ValueError(f"{r['mode_id']}: EN/VI names required")
            if int(r["team_size"])!=5 or int(r["energy_cost"])<=0 or int(r["daily_attempt_limit"])<=0: raise ValueError(f"{r['mode_id']}: invalid team/Energy/attempt contract")
            if r["reset_policy"]!="DAILY_UTC": raise ValueError(f"{r['mode_id']}: reset must be DAILY_UTC")
            if a.enforce and r["release_status"]!="PRODUCTION_READY": raise ValueError(f"{r['mode_id']}: release status not ready")
            if r["mode_type"] in LEGACY: raise ValueError(f"{r['mode_id']}: legacy mode type used in release census")
        by={mode:[] for mode in EXPECTED}
        for e in enemies:
            if e["mode_id"] not in by: raise ValueError(f"enemy references unknown mode {e['mode_id']}")
            by[e["mode_id"]].append(e)
        for mode,team in by.items():
            if len(team)!=5 or sorted(int(x["slot"]) for x in team)!=list(range(5)): raise ValueError(f"{mode}: requires five contiguous enemy slots")
        require("server/src/main/resources/db/migration/V14__resource_pve_runs.sql","resource_pve_runs","request_id uuid primary key","result_json")
        require("server/src/main/java/com/ninjaassemble/pve/application/ResourcePveCatalogService.java","RELEASE_MODE_COUNT = 9","DAILY_UTC","PRODUCTION_READY")
        require("server/src/main/java/com/ninjaassemble/pve/application/ResourcePveApplicationService.java","pve_mode_progress","pg_advisory_xact_lock","RealtimeBattleEngine","resource-pve:","result_json","attemptsRemaining","withReplayed(true)")
        require("server/src/main/java/com/ninjaassemble/pve/api/ResourcePveController.java",'"/{modeId}/battle"',"requestId is required")
        require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs","ResourcePveModeDto","ResourcePveBoardDto","ResourcePveBattleDto")
        require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs","GetResourcePveAsync","PlayResourcePveAsync")
        require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs","ResourcePve","RecommendedResourcePve","BattleResourcePveAsync","RefreshResourcePveAsync")
        print("M62_RESOURCE_PVE_OK modes=9 enemies=45 reset=DAILY_UTC attempts=server reward=idempotent battle=realtime client=1")
        return 0
    except (ValueError,KeyError) as e:
        print(f"M62_RESOURCE_PVE_INVALID {e}",file=sys.stderr);return 1
if __name__=="__main__":raise SystemExit(main())
