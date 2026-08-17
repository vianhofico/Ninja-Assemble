#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "server/src/main/java"
SCRIPTS_ROOT = ROOT / "scripts"
LEGACY_ENGINE = JAVA_ROOT / "com/ninjaassemble/battle/sim/DeterministicBattleEngine.java"
REALTIME_EXECUTOR = JAVA_ROOT / "com/ninjaassemble/battle/sim/RealtimeBattleExecutor.java"


def occurrences(root: Path, token: str, suffix: str) -> list[Path]:
    found: list[Path] = []
    for path in root.rglob(f"*{suffix}"):
        if path.is_file() and token in path.read_text(encoding="utf-8"):
            found.append(path)
    return found


def relative(paths: list[Path]) -> list[str]:
    return [str(path.relative_to(ROOT)) for path in sorted(paths)]


def main() -> int:
    if not LEGACY_ENGINE.exists():
        print("LEGACY_COMBAT_REMOVAL_READY old_engine_already_deleted=1")
        return 0

    production_refs = [
        path for path in occurrences(JAVA_ROOT, "DeterministicBattleEngine", ".java")
        if path != LEGACY_ENGINE
    ]
    if production_refs:
        print("LEGACY_COMBAT_PRODUCTION_REFERENCES", relative(production_refs))
        return 1

    self_path = Path(__file__).resolve()
    script_refs = [
        path for path in occurrences(SCRIPTS_ROOT, "DeterministicBattleEngine.java", ".py")
        if path.resolve() != self_path
    ]
    if script_refs:
        print("LEGACY_COMBAT_VALIDATOR_REFERENCES", relative(script_refs))
        return 1

    test_refs = occurrences(ROOT / "server/src/test/java", "DeterministicBattleEngine", ".java")
    bridge = REALTIME_EXECUTOR.read_text(encoding="utf-8") if REALTIME_EXECUTOR.exists() else ""
    bridge_present = "new BattleRequest(" in bridge

    print(
        "LEGACY_COMBAT_REMOVAL_READY",
        f"production_refs=0 validator_refs=0 legacy_test_files={len(test_refs)} request_bridge={1 if bridge_present else 0}",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
