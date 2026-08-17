#!/usr/bin/env python3
"""Static contract checks for playable Shadow Arena on the shared realtime combat core."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"SHADOW_ARENA_CONTRACT_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/battle/domain/BattleRules.java",
            "SHADOW_SQUAD_SIZE = 5", "SHADOW_SQUAD_COUNT = 3", "SHADOW_ROSTER_SIZE", "SHADOW_WINS_REQUIRED = 2")
    require("server/src/main/resources/db/migration/V5__pvp_arena.sql",
            "shadow_arena_profiles", "shadow_arena_battles", "roster_snapshot jsonb", "squad_results jsonb")
    require("server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleEngine.java",
            "Authoritative deterministic continuous-time auto-combat simulation", "PriorityQueue<ScheduledEvent>")
    require("server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleRequest.java", "record RealtimeBattleRequest")
    require("server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java",
            "BattleRules.SHADOW_ROSTER_SIZE", "new ShadowArenaRosterSnapshot", "ShadowArenaMatchResolver",
            "bo3-hp-power-realtime-v2", "RealtimeBattleEngine", "RealtimeBattleRequest", "BattleRuleset.experimentalV1()", "durationMs()",
            "TOTAL_HP", "SQUAD_POWER", "Currency.SHADOW_COIN",
            "training ? 0", "shadow_arena_profiles", "shadow_arena_battles", "cast(? as jsonb)")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/shadow-arena"', '"/shadow-arena/{opponentPlayerId}/battle"')
    require("server/src/test/java/com/ninjaassemble/pvp/domain/ShadowArenaMatchResolverTest.java",
            "resolvesBestOfThreeAsSoonAsEitherSideReachesTwoWins", "SeriesWinner.PLAYER", "SeriesWinner.OPPONENT")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
            "ShadowArenaStateDto", "ShadowArenaOpponentDto", "ShadowSquadBattleDto", "ShadowArenaBattleDto")
    require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
            "GetShadowArenaAsync", "FightShadowArenaAsync")
    require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
            "ShadowArenaStateDto ShadowArena", "RecommendedShadowOpponent", "RefreshShadowArenaAsync", "FightShadowArenaAsync")
    require("client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
            "ScreenId.ShadowArena", "BuildShadowArena", "PresentShadowArenaBattle", "NEED {shadow.missingCount} NINJA",
            "rating/reward unchanged")
    print("PLAYABLE_SHADOW_ARENA_OK eligibility=15 squads=3x5 wins=2 tiebreak=hp>power combat=RealtimeBattleEngine")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
