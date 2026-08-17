#!/usr/bin/env python3
"""M49 continuous-time combat contract gate.

Rejects turn/round timing concepts from authoritative runtime code/data while allowing unrelated prose/tests to be
classified in the migration audit document.
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FORBIDDEN = [
    r"\bdurationTurns\b", r"\bduration_turns\b", r"\bremainingTurns\b", r"\bmaxRounds\b",
    r"\bTURN_START\b", r"\bTURN_END\b", r"\bROUND_START\b", r"\bROUND_END\b", r"\badvanceStatuses\b",
]
RUNTIME_ROOTS = [
    ROOT / "server/src/main/java/com/ninjaassemble/battle",
    ROOT / "server/src/main/java/com/ninjaassemble/hero/domain",
    ROOT / "server/src/main/java/com/ninjaassemble/play/domain",
    ROOT / "game-data/skills",
]


def fail(message: str) -> None:
    raise SystemExit(f"M49_REALTIME_INVALID {message}")


def main() -> int:
    violations: list[str] = []
    for root in RUNTIME_ROOTS:
        for path in sorted(p for p in root.rglob("*") if p.suffix in {".java", ".csv"}):
            text = path.read_text(encoding="utf-8")
            for pattern in FORBIDDEN:
                if re.search(pattern, text):
                    violations.append(f"{path.relative_to(ROOT)}:{pattern}")
    if violations:
        fail("deprecated turn timing remains: " + ", ".join(violations[:20]))

    engine = (ROOT / "server/src/main/java/com/ninjaassemble/battle/sim/DeterministicBattleEngine.java").read_text(encoding="utf-8")
    for token in ["PriorityQueue<ScheduledEvent>", "timestampMs", "maxBattleDurationMs", "attackIntervalMs", "RAGE_FULL", "RAGE_SKILL_READY"]:
        if token not in engine:
            fail(f"engine missing {token}")
    if re.search(r"for\s*\([^\)]*round", engine, re.I):
        fail("authoritative round loop still present")
    if "Thread.sleep" in engine or "System.currentTimeMillis" in engine or "System.nanoTime" in engine:
        fail("wall-clock/thread timing is forbidden")

    effects = (ROOT / "game-data/skills/technique-effects.csv").read_text(encoding="utf-8").splitlines()[0]
    if "duration_ms" not in effects or "tick_interval_ms" not in effects or "duration_turns" in effects:
        fail("technique effect schema is not real-time")

    for required in [
        "game-data/combat/rage-rules.csv",
        "game-data/combat/timing-rules.csv",
        "docs/combat/REALTIME_COMBAT_MIGRATION_AUDIT.md",
        "docs/combat/TIME_BASED_SKILL_AUDIT.md",
    ]:
        if not (ROOT / required).exists():
            fail(f"missing {required}")

    print("M49_REALTIME_OK scheduler=event_queue timing=milliseconds resource=rage")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
