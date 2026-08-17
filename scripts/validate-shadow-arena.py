#!/usr/bin/env python3
"""Static contract checks for playable Shadow Arena after realtime combat cutover."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"SHADOW_ARENA_CONTRACT_INVALID {path} missing={missing}")


def forbid(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    leaked = [token for token in tokens if token in text]
    if leaked:
        raise SystemExit(f"SHADOW_ARENA_CONTRACT_LEGACY_RUNTIME {path} leaked={leaked}")


def main() -> int:
    service = "server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java"
    require("server/src/main/java/com/ninjaassemble/battle/domain/BattleRules.java",
            "SHADOW_SQUAD_SIZE = 5", "SHADOW_SQUAD_COUNT = 3", "SHADOW_ROSTER_SIZE", "SHADOW_WINS_REQUIRED = 2")
    require("server/src/main/resources/db/migration/V5__pvp_arena.sql",
            "shadow_arena_profiles", "shadow_arena_battles", "roster_snapshot jsonb", "squad_results jsonb")
    require(service,
            "BattleRules.SHADOW_ROSTER_SIZE", "new ShadowArenaRosterSnapshot", "ShadowArenaMatchResolver",
            "bo3-hp-power-tiebreak-realtime-v2", "TOTAL_HP", "SQUAD_POWER", "Currency.SHADOW_COIN",
            "RealtimeBattleExecutor", "new RealtimeBattleRequest", "RealtimeBattleCompatibilityAdapter.project",
            "RealtimeBattleResult realtimeBattle", "durationMs", "rulesetVersion",
            "training ? 0", "shadow_arena_profiles", "shadow_arena_battles", "cast(? as jsonb)")
    forbid(service, "DeterministicBattleEngine", "new BattleRequest(", "BattleRuleset.experimentalV1()")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/shadow-arena"', '"/shadow-arena/{opponentPlayerId}/battle"')
    require("server/src/test/java/com/ninjaassemble/pvp/domain/ShadowArenaMatchResolverTest.java",
            "resolvesBestOfThreeAsSoonAsEitherSideReachesTwoWins", "SeriesWinner.PLAYER", "SeriesWinner.OPPONENT")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
            "ShadowArenaStateDto", "ShadowArenaOpponentDto", "ShadowSquadBattleDto", "ShadowArenaBattleDto", "realtimeBattle")
    require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
            "GetShadowArenaAsync", "FightShadowArenaAsync")
    require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
            "ShadowArenaStateDto ShadowArena", "RecommendedShadowOpponent", "RefreshShadowArenaAsync", "FightShadowArenaAsync",
            "RealtimeBattleDtoCompatibility.Promote(await api.FightShadowArenaAsync")
    require("client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
            "ScreenId.ShadowArena", "BuildShadowArena", "PresentShadowArenaBattle", "NEED {shadow.missingCount} NINJA",
            "rating/reward unchanged")
    print("PLAYABLE_SHADOW_ARENA_OK eligibility=15 squads=3x5 realtime=authoritative timestamp_replay=yes tiebreak=hp>power")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
