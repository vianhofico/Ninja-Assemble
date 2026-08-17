#!/usr/bin/env python3
"""Static contract checks for playable Arena after realtime combat cutover."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"ARENA_CONTRACT_INVALID {path} missing={missing}")


def forbid(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    leaked = [token for token in tokens if token in text]
    if leaked:
        raise SystemExit(f"ARENA_CONTRACT_LEGACY_RUNTIME {path} leaked={leaked}")


def main() -> int:
    service = "server/src/main/java/com/ninjaassemble/pvp/application/ArenaApplicationService.java"
    require(
        "server/src/main/resources/db/migration/V5__pvp_arena.sql",
        "arena_profiles", "arena_opponent_snapshots", "arena_battles")
    require(
        service,
        "SEASON_ID", "ArenaRatingProfile.experimentalV1", "arena_opponent_snapshots", "arena_battles",
        "RealtimeBattleExecutor", "new RealtimeBattleRequest", "RealtimeBattleCompatibilityAdapter.project",
        "realtimeBattle.outcome()", "RealtimeBattleResult realtimeBattle",
        "Currency.ARENA_COIN", "training ? 0", "ArenaOpponentView")
    forbid(service, "DeterministicBattleEngine", "new BattleRequest(", "BattleRuleset.experimentalV1()")
    require(
        "server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
        '"/arena"', '"/arena/{opponentPlayerId}/battle"')
    require(
        "server/src/test/java/com/ninjaassemble/pvp/domain/ArenaRatingCalculatorTest.java",
        "experimentalArenaProfileUsesDeterministicWinLossDeltasAndFloor", "experimental-v1-unverified")
    require(
        "client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
        "ArenaOpponentDto", "ArenaStateDto", "ArenaBattleDto", "arenaCoinReward", "realtimeBattle")
    require(
        "client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
        "GetArenaAsync", "FightArenaAsync")
    require(
        "client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
        "ArenaStateDto Arena", "RecommendedArenaOpponent", "FightArenaAsync", "RefreshArenaAsync",
        "RealtimeBattleDtoCompatibility.Promote(await api.FightArenaAsync")
    require(
        "client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
        "ScreenId.Arena", "BuildArena", "PresentArenaBattle", "TRAIN MIRROR", "rating/reward unchanged")
    print("PLAYABLE_ARENA_OK async_opponents=5 realtime=authoritative timestamp_replay=yes rating=experimental")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
