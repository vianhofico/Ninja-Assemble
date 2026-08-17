#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "game-data/skills/technique-effects.csv"
SKILL_EFFECT = ROOT / "server/src/main/java/com/ninjaassemble/hero/domain/SkillEffectDefinition.java"
TECHNIQUE = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/TechniqueEffectResolver.java"
PASSIVE = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/PassiveEffectResolver.java"
ABILITY = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/ExperimentalAbilityProfile.java"
BATTLE_ABILITY = ROOT / "server/src/main/java/com/ninjaassemble/battle/sim/BattleAbility.java"


def fail(message: str) -> int:
    print("REALTIME_CONTENT_TIMING_FAIL", message)
    return 1


def main() -> int:
    for path in (CSV_PATH, SKILL_EFFECT, TECHNIQUE, PASSIVE, ABILITY, BATTLE_ABILITY):
        if not path.exists():
            return fail(f"missing {path.relative_to(ROOT)}")

    with CSV_PATH.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        rows = list(reader)
        header = reader.fieldnames or []

    if "duration_turns" in header:
        return fail("technique-effects.csv still authors duration_turns")
    for required in ("duration_ms", "tick_interval_ms"):
        if required not in header:
            return fail(f"technique-effects.csv missing {required}")

    for row in rows:
        if row.get("status") != "EXPERIMENTAL_RUNTIME":
            continue
        effect_type = (row.get("effect_type") or "").strip().upper()
        status_id = (row.get("status_id") or "").strip().upper()
        duration_ms = int(row.get("duration_ms") or 0)
        tick_ms = int(row.get("tick_interval_ms") or 0)
        if effect_type == "STATUS" and duration_ms <= 0:
            return fail(f"{row.get('technique_id')} status {status_id} has no duration_ms")
        if status_id in {"BURN", "POISON", "BLEED"} and tick_ms <= 0:
            return fail(f"{row.get('technique_id')} DOT {status_id} has no tick_interval_ms")

    skill_effect = SKILL_EFFECT.read_text(encoding="utf-8")
    technique = TECHNIQUE.read_text(encoding="utf-8")
    passive = PASSIVE.read_text(encoding="utf-8")
    ability = ABILITY.read_text(encoding="utf-8")
    battle_ability = BATTLE_ABILITY.read_text(encoding="utf-8")

    for source_name, source in (("SkillEffectDefinition", skill_effect), ("TechniqueEffectResolver", technique), ("PassiveEffectResolver", passive)):
        if "durationTurns" in source or "duration_turns" in source:
            return fail(f"{source_name} contains turn-authored timing")

    for marker in ("durationMs", "tickIntervalMs"):
        if marker not in skill_effect or marker not in technique:
            return fail(f"millisecond effect contract missing {marker}")
    for marker in ("cooldownMs", "castTimeMs", "recoveryMs"):
        if marker not in battle_ability or marker not in ability:
            return fail(f"explicit ability timing missing {marker}")
    if "BattleAbilityKind.RAGE_SKILL" not in ability:
        return fail("ability profile lost Rage Skill mapping")

    print(f"REALTIME_CONTENT_TIMING_OK rows={len(rows)} duration_ms=1 tick_interval_ms=1 rage_skill=1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
