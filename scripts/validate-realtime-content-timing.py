#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "game-data/skills/technique-effects.csv"
TECHNIQUE = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/TechniqueEffectResolver.java"
PASSIVE = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/PassiveEffectResolver.java"
ABILITY = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/ExperimentalAbilityProfile.java"


def fail(message: str) -> int:
    print("REALTIME_CONTENT_TIMING_FAIL", message)
    return 1


def main() -> int:
    for path in (CSV_PATH, TECHNIQUE, PASSIVE, ABILITY):
        if not path.exists():
            return fail(f"missing {path.relative_to(ROOT)}")

    with CSV_PATH.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
        header = list(rows[0].keys()) if rows else []

    if "duration_turns" in header:
        return fail("technique-effects.csv still authors duration_turns")
    for required in ("duration_ms", "tick_interval_ms"):
        if required not in header:
            return fail(f"technique-effects.csv missing {required}")

    for row in rows:
        if row["status"] != "EXPERIMENTAL_RUNTIME":
            continue
        effect_type = row["effect_type"]
        status_id = row["status_id"].strip().upper()
        duration_ms = int(row["duration_ms"] or 0)
        tick_ms = int(row["tick_interval_ms"] or 0)
        if effect_type == "STATUS" and duration_ms <= 0:
            return fail(f"{row['technique_id']} status {status_id} has no explicit duration_ms")
        if status_id in {"BURN", "POISON", "BLEED"} and tick_ms <= 0:
            return fail(f"{row['technique_id']} DOT {status_id} has no explicit tick_interval_ms")

    technique = TECHNIQUE.read_text(encoding="utf-8")
    passive = PASSIVE.read_text(encoding="utf-8")
    ability = ABILITY.read_text(encoding="utf-8")

    if "durationTurns" in technique or "durationTurns" in passive:
        return fail("production effect resolver references durationTurns")
    for marker in ("durationMs", "tickIntervalMs", "cells.length != 15"):
        if marker not in technique:
            return fail(f"TechniqueEffectResolver missing {marker}")
    for marker in ("LONG_BATTLE_BUFF_MS", "MEDIUM_BUFF_MS", "SHORT_BUFF_MS"):
        if marker not in passive:
            return fail(f"PassiveEffectResolver missing {marker}")
    for marker in ("timing.cooldownMs()", "timing.castTimeMs()", "timing.recoveryMs()"):
        if marker not in ability:
            return fail(f"ExperimentalAbilityProfile missing {marker}")

    print(f"REALTIME_CONTENT_TIMING_OK curated_rows={len(rows)} explicit_status_ms=1 explicit_ability_timing=1 explicit_passive_ms=1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
