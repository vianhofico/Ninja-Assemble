#!/usr/bin/env python3
"""Validate structured effects after M49 real-time migration."""
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "game-data/skills/technique-effects.csv"


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"STRUCTURED_EFFECTS_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/hero/domain/EffectType.java",
            "DAMAGE", "HEAL", "RAGE", "STATUS", "CLEANSE", "DISPEL", "REVIVE", "SHIELD")
    require("server/src/main/java/com/ninjaassemble/hero/domain/SkillEffectDefinition.java",
            "long durationMs", "long tickIntervalMs", "durationMs < 0", "tickIntervalMs < 0")
    require("server/src/main/java/com/ninjaassemble/battle/sim/DeterministicBattleEngine.java",
            "PriorityQueue<ScheduledEvent>", "STATUS_TICK", "STATUS_EXPIRE", "hasStatus(\"STUN\"", "hasStatus(\"SILENCE\"")
    require("client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs", "STATUS_TICK")

    with DATA.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))
        fields = handle.seek(0)  # no-op: keeps linter quiet in minimal script
    if not rows:
        raise SystemExit("STRUCTURED_EFFECTS_INVALID no technique effects")
    required = {"duration_ms", "tick_interval_ms", "effect_type", "status_id"}
    header = set(rows[0].keys())
    if "duration_turns" in header or not required.issubset(header):
        raise SystemExit(f"STRUCTURED_EFFECTS_INVALID header={sorted(header)}")
    for row in rows:
        duration = int(row["duration_ms"] or 0)
        interval = int(row["tick_interval_ms"] or 0)
        if duration < 0 or interval < 0:
            raise SystemExit(f"STRUCTURED_EFFECTS_INVALID negative timing {row['technique_id']}")
        status = row["status_id"].strip()
        effect_type = row["effect_type"].strip()
        if effect_type == "STATUS" and status and duration <= 0:
            raise SystemExit(f"STRUCTURED_EFFECTS_INVALID timed status without duration {row['technique_id']}:{status}")
        if status in {"BURN", "POISON", "BLEED"} and interval <= 0:
            raise SystemExit(f"STRUCTURED_EFFECTS_INVALID DOT without tick interval {row['technique_id']}:{status}")
        if interval > 0 and duration <= 0:
            raise SystemExit(f"STRUCTURED_EFFECTS_INVALID periodic effect without duration {row['technique_id']}")
    print(f"STRUCTURED_EFFECTS_OK rows={len(rows)} timing=milliseconds")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
