#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ARENA = ROOT / "server/src/main/java/com/ninjaassemble/pvp/application/ArenaApplicationService.java"
SHADOW = ROOT / "server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java"
UNITY_COMPAT = ROOT / "client-unity/Assets/Scripts/Game/Network/RealtimeBattleDtoCompatibility.cs"


def require(content: str, path: Path, markers: list[str]) -> None:
    for marker in markers:
        if marker not in content:
            raise RuntimeError(f"REALTIME_COMPETITIVE_MISSING_MARKER {path.relative_to(ROOT)} :: {marker}")


def forbid(content: str, path: Path, markers: list[str]) -> None:
    for marker in markers:
        if marker in content:
            raise RuntimeError(f"REALTIME_COMPETITIVE_LEGACY_RUNTIME {path.relative_to(ROOT)} :: {marker}")


def main() -> int:
    for path in (ARENA, SHADOW, UNITY_COMPAT):
        if not path.exists():
            print("REALTIME_COMPETITIVE_MISSING_FILE", path.relative_to(ROOT))
            return 1

    arena = ARENA.read_text(encoding="utf-8")
    shadow = SHADOW.read_text(encoding="utf-8")
    unity = UNITY_COMPAT.read_text(encoding="utf-8")

    require(arena, ARENA, [
        "RealtimeBattleExecutor",
        "new RealtimeBattleRequest",
        "RealtimeBattleCompatibilityAdapter.project",
        "realtimeBattle.outcome()",
        "RealtimeBattleResult realtimeBattle",
    ])
    require(shadow, SHADOW, [
        "RealtimeBattleExecutor",
        "new RealtimeBattleRequest",
        "RealtimeBattleCompatibilityAdapter.project",
        "squadDecision(realtimeBattle",
        "RealtimeBattleResult realtimeBattle",
        "durationMs",
        "rulesetVersion",
    ])
    forbid(arena, ARENA, ["DeterministicBattleEngine", "new BattleRequest(", "BattleRuleset.experimentalV1()"])
    forbid(shadow, SHADOW, ["DeterministicBattleEngine", "new BattleRequest(", "BattleRuleset.experimentalV1()"])

    require(unity, UNITY_COMPAT, ["Promote(ArenaBattleDto result)", "Promote(ShadowArenaBattleDto result)"])

    print("REALTIME_COMPETITIVE_OK arena=1 shadow=1 one_pass=1 unity_promote=1")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as exc:
        print(exc)
        raise SystemExit(1)
