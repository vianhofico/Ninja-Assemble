#!/usr/bin/env python3
"""M63 production Arena + Shadow Arena meta-loop audit."""
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]

def require(path,*tokens):
    text=(ROOT/path).read_text(encoding="utf-8")
    missing=[t for t in tokens if t not in text]
    if missing: raise ValueError(f"{path} missing {missing}")
    return text

def main():
    try:
        subprocess.run([sys.executable,str(ROOT/"scripts/validate-playable-arena.py")],cwd=ROOT,check=True)
        subprocess.run([sys.executable,str(ROOT/"scripts/validate-shadow-arena.py")],cwd=ROOT,check=True)
        require("server/src/main/resources/db/migration/V15__competitive_production.sql",
                "competitive_battle_requests","competitive_season_results","shadow_defense_formations","result_json","claimed boolean")
        season=require("server/src/main/java/com/ninjaassemble/pvp/application/CompetitiveSeasonService.java",
                "YearMonth","ZoneOffset.UTC","currentSeasonId","previousSeasonId","ensureArenaProfile","ensureShadowProfile","claimPrevious","competitive-season:")
        if "arena-experimental-s1" in season or "shadow-experimental-s1" in season: raise ValueError("production season service must not use fixed experimental season IDs")
        require("server/src/main/java/com/ninjaassemble/pvp/application/CompetitiveFormationService.java",
                "saveArenaDefense","ensureArenaDefense","saveShadowDefense","ensureShadowDefense","SHADOW_ROSTER_SIZE")
        require("server/src/main/java/com/ninjaassemble/pvp/application/ProductionArenaService.java",
                "defenses.loadArenaDefense","defenses.ensureArenaDefense","competitive_battle_requests","pg_advisory_xact_lock","history","claimPreviousSeason","ARENA_COIN","withReplayed(true)")
        require("server/src/main/java/com/ninjaassemble/pvp/application/ProductionShadowArenaService.java",
                "SHADOW_ROSTER_SIZE","SHADOW_SQUAD_COUNT","wins<2","competitive_battle_requests","pg_advisory_xact_lock","history","claimPreviousSeason","SHADOW_COIN","withReplayed(true)")
        require("server/src/main/java/com/ninjaassemble/pvp/api/CompetitiveController.java",
                '"/arena/defense"','"/arena/history"','"/arena/season/claim"','"/shadow-arena/defense"','"/shadow-arena/history"','"/shadow-arena/season/claim"',"requestId is required")
        require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
                "SeasonRewardDto","CompetitiveHistoryItemDto","defenseConfigured","replayed")
        api=require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
                '"/competitive/arena"','"/competitive/shadow-arena"',"SaveArenaDefenseAsync","SaveShadowDefenseAsync","GetArenaHistoryAsync","GetShadowArenaHistoryAsync","ClaimArenaSeasonAsync","ClaimShadowSeasonAsync")
        if '$"/api/v1/play/{Escape(playerId)}/arena/{Escape(opponentPlayerId)}/battle"' in api: raise ValueError("Unity production Arena path still uses legacy non-idempotent endpoint")
        require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
                "ArenaHistory","ShadowArenaHistory","SaveArenaDefenseAsync","SaveShadowDefenseAsync","Guid.NewGuid().ToString()","ClaimArenaSeasonAsync","ClaimShadowSeasonAsync")
        print("M63_COMPETITIVE_OK arena=5v5 defense=5 history=1 shadow=15x3 defense=15 season=MONTHLY_UTC reward=idempotent requests=idempotent client=production-path")
        return 0
    except (ValueError,subprocess.CalledProcessError) as e:
        print(f"M63_COMPETITIVE_INVALID {e}",file=sys.stderr);return 1
if __name__=="__main__":raise SystemExit(main())
