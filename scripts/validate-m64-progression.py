#!/usr/bin/env python3
from __future__ import annotations
import csv
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
EXPECTED_BEASTS={"shukaku","matatabi","isobu","son-goku","kokuo","saiken","chomei","gyuki","kurama"}
EXPECTED_LEARNING={"scroll-mastery","ninja-college"}
def rows(path):
    with (ROOT/path).open(encoding="utf-8-sig",newline="") as h:return list(csv.DictReader(h))
def require(path,*tokens):
    text=(ROOT/path).read_text(encoding="utf-8");missing=[t for t in tokens if t not in text]
    if missing:raise ValueError(f"{path} missing {missing}")
def main():
    try:
        subprocess.run([sys.executable,str(ROOT/"scripts/validate-hero-progression.py")],cwd=ROOT,check=True)
        subprocess.run([sys.executable,str(ROOT/"scripts/validate-playable-equipment.py")],cwd=ROOT,check=True)
        require("server/src/main/java/com/ninjaassemble/progression/application/FrameAdvanceApplicationService.java","advance","FrameTier")
        tracks=rows("game-data/progression/advanced-tracks.csv")
        if len(tracks)!=11:raise ValueError(f"expected 11 advanced tracks, found {len(tracks)}")
        ids={r["track_id"].strip() for r in tracks};beasts={r["track_id"].strip() for r in tracks if r["track_type"].strip()=="JINCHURIKI"}
        if len(ids)!=11:raise ValueError("duplicate advanced progression track")
        if beasts!=EXPECTED_BEASTS:raise ValueError(f"Tailed-Beast census mismatch: {sorted(beasts)}")
        if ids-beasts!=EXPECTED_LEARNING:raise ValueError(f"learning census mismatch: {sorted(ids-beasts)}")
        for r in tracks:
            label=r["track_id"]
            if not r["name_en"].strip() or not r["name_vi"].strip():raise ValueError(f"{label}: EN/VI names required")
            if int(r["max_level"])<=0 or int(r["min_player_level"])<=0:raise ValueError(f"{label}: invalid level contract")
            if min(int(r[x]) for x in ("gold_base","gold_growth","item_base","item_growth"))<0:raise ValueError(f"{label}: negative cost")
            if not r["item_id"].strip() or not r["bonus_stat"].strip() or int(r["bonus_per_level"])<=0:raise ValueError(f"{label}: item/bonus required")
            if r["release_status"]!="PRODUCTION_READY":raise ValueError(f"{label}: not production ready")
        require("server/src/main/resources/db/migration/V17__advanced_progression.sql","player_progression_tracks","progression_upgrade_requests","request_id uuid primary key","result_json")
        require("server/src/main/java/com/ninjaassemble/progression/application/AdvancedProgressionCatalogService.java","RELEASE_TRACK_COUNT = 11","learning != 2","beasts != 9","goldCost","itemCost","cumulativeBonus")
        require("server/src/main/java/com/ninjaassemble/progression/application/AdvancedProgressionApplicationService.java","pg_advisory_xact_lock","progression:","Currency.GOLD","inventory.mutate","for update","withReplayed(true)","result_json")
        require("server/src/main/java/com/ninjaassemble/progression/api/AdvancedProgressionController.java",'"/{trackId}/upgrade"',"requestId is required")
        require("client-unity/Assets/Scripts/Game/Progression/AdvancedProgressionDtos.cs","AdvancedProgressionBoardDto","AdvancedProgressionUpgradeDto","cumulativeBonus")
        require("client-unity/Assets/Scripts/Game/Progression/AdvancedProgressionClient.cs","GetBoardAsync","UpgradeAsync",'"/progression/advanced"')
        require("client-unity/Assets/Scripts/Game/Progression/AdvancedProgressionStore.cs","RecommendedTrack","Guid.NewGuid().ToString()","RefreshAsync")
        print("M64_PROGRESSION_OK frame=1 hero_level=1 equipment=1 learning=2 tailed_beasts=9 upgrades=idempotent client_module=1")
        return 0
    except (ValueError,subprocess.CalledProcessError,KeyError) as e:
        print(f"M64_PROGRESSION_INVALID {e}",file=sys.stderr);return 1
if __name__=="__main__":raise SystemExit(main())
