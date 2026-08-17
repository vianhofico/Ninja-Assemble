#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"REALTIME_COMPETITIVE_INVALID {path} missing={missing}")


def forbid(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    leaked = [token for token in tokens if token in text]
    if leaked:
        raise SystemExit(f"REALTIME_COMPETITIVE_LEGACY {path} leaked={leaked}")


def main() -> int:
    engine = "server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleEngine.java"
    request = "server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleRequest.java"
    rules = "server/src/main/java/com/ninjaassemble/battle/sim/BattleRuleset.java"
    arena = "server/src/main/java/com/ninjaassemble/pvp/application/ArenaApplicationService.java"
    shadow = "server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java"

    require(engine,
            "Authoritative deterministic continuous-time auto-combat simulation",
            "PriorityQueue<ScheduledEvent>", "ScheduledType", "timestampMs", "RAGE_SKILL_READY",
            "simulate(RealtimeBattleRequest request)")
    forbid(engine, "for (int round", "maxRounds", "durationTurns")
    require(request, "record RealtimeBattleRequest", "BattleRuleset ruleset", "List<BattleUnitSeed> units")
    require(rules, "maxBattleDurationMs", "attackIntervalMs", "rageSkillCost")
    forbid(rules, "maxRounds")

    require(arena, "RealtimeBattleEngine", "RealtimeBattleRequest", "BattleRuleset.experimentalV1()", "battle.outcome()", "Currency.ARENA_COIN")
    require(shadow, "RealtimeBattleEngine", "RealtimeBattleRequest", "BattleRuleset.experimentalV1()", "durationMs()", "TOTAL_HP", "SQUAD_POWER")
    old_engine = "Deterministic" + "BattleEngine"
    old_constructor = "new " + "Battle" + "Request("
    forbid(arena, old_engine, old_constructor)
    forbid(shadow, old_engine, old_constructor)

    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
            "timestampMs", "durationMs", "ArenaBattleDto", "ShadowSquadBattleDto")
    require("client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs",
            "Time.unscaledDeltaTime", "SetPlaybackSpeed")

    print("REALTIME_COMPETITIVE_OK shared_core=RealtimeBattleEngine arena=1 shadow=1 timestamp_replay=1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
