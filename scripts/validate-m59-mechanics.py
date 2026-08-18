#!/usr/bin/env python3
"""M59 executable mechanics and realtime timing audit.

The generated M50 candidate proves structural executability, but generated defaults are
not equivalent to mechanics review. M59 therefore keeps an explicit evidence-backed
review registry. Normal mode validates canonical realtime mechanics and reports review
debt; --enforce requires a reviewed record for every base and Awakening skill.
"""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GENERATOR = ROOT / "scripts/generate-m50-skill-design.py"
M50_VALIDATOR = ROOT / "scripts/validate-m50-skill-design.py"
EFFECT_VALIDATOR = ROOT / "scripts/validate-technique-effect-coverage.py"
REVIEWS = ROOT / "game-data/skills/m59-mechanics-reviews.csv"
AWAKENINGS = ROOT / "game-data/skills/awakening-skills.csv"
LIBRARIES = [ROOT / f"game-data/skills/technique-library-0{i}.csv" for i in range(1, 5)]
EXPECTED_HEROES = 194
EXPECTED_BASE = 970
EXPECTED_AWAKENINGS = 60
FORBIDDEN_TRIGGERS = {"TURN_START", "TURN_END", "ROUND_START", "ROUND_END"}
NUMERIC_FIELDS = ("rage_cost", "rage_gain", "cooldown_ms", "cast_time_ms", "impact_ms", "recovery_ms")
REVIEW_FIELDS = {
    "review_id", "skill_type", "hero_id", "skill_key", "status", "ability_kind", "trigger_type",
    "rage_cost", "rage_gain", "cooldown_ms", "cast_time_ms", "impact_ms", "recovery_ms",
    "target_selector", "effect_profile_id", "special_mechanic", "evidence_ref", "review_note",
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def int_field(row: dict[str, str], field: str, label: str) -> int:
    try:
        value = int((row.get(field) or "").strip())
    except ValueError as exc:
        raise ValueError(f"{label}: {field} must be an integer") from exc
    if value < 0:
        raise ValueError(f"{label}: {field} must be >= 0")
    return value


def technique_ids() -> set[str]:
    result: set[str] = set()
    for path in LIBRARIES:
        for row in rows(path):
            technique_id = (row.get("technique_id") or "").strip()
            if not technique_id or technique_id in result:
                raise ValueError(f"blank/duplicate technique id: {technique_id!r}")
            result.add(technique_id)
    return result


def validate_base_mechanics(row: dict[str, str], techniques: set[str]) -> None:
    hero_id = row["hero_id"].strip(); slot = row["slot"].strip(); label = f"{hero_id}/{slot}"
    for field in NUMERIC_FIELDS:
        int_field(row, field, label)
    trigger = row["trigger_type"].strip().upper()
    if trigger in FORBIDDEN_TRIGGERS or "TURN" in trigger or "ROUND" in trigger:
        raise ValueError(f"{label}: non-realtime trigger {trigger}")
    if not row["target_selector"].strip():
        raise ValueError(f"{label}: target_selector required")
    effect_profile = row["effect_profile_id"].strip()
    if effect_profile not in techniques:
        raise ValueError(f"{label}: unknown effect/technique profile {effect_profile!r}")
    if slot == "BASIC":
        if row["ability_kind"] != "BASIC" or row["trigger_type"] != "BASIC_AUTO":
            raise ValueError(f"{label}: invalid BASIC mechanics")
        if int_field(row, "rage_gain", label) <= 0:
            raise ValueError(f"{label}: BASIC must generate Rage")
    elif slot == "SKILL_1":
        if row["ability_kind"] != "RAGE_SKILL" or row["trigger_type"] != "RAGE_FULL":
            raise ValueError(f"{label}: Skill 1 must be Rage Skill")
        if int_field(row, "rage_cost", label) != 100:
            raise ValueError(f"{label}: Rage Skill must cost 100 Rage")
    elif slot in {"SKILL_2", "SKILL_3"}:
        if row["ability_kind"] != "ACTIVE_SKILL":
            raise ValueError(f"{label}: active slot must be ACTIVE_SKILL")
        if int_field(row, "cooldown_ms", label) <= 0:
            raise ValueError(f"{label}: active skill requires positive cooldown_ms")
    elif slot == "PASSIVE":
        if row["ability_kind"] != "PASSIVE":
            raise ValueError(f"{label}: passive ability kind invalid")


def validate_review(row: dict[str, str], base_by_key: dict[tuple[str, str], dict[str, str]], awakening_keys: set[tuple[str, str]], techniques: set[str]) -> tuple[str, str]:
    missing = sorted(field for field in REVIEW_FIELDS if field not in row)
    if missing:
        raise ValueError(f"m59-mechanics-reviews.csv missing columns {missing}")
    review_id = row["review_id"].strip(); skill_type = row["skill_type"].strip().upper()
    hero_id = row["hero_id"].strip(); skill_key = row["skill_key"].strip(); status = row["status"].strip().upper()
    label = f"{skill_type}:{hero_id}/{skill_key}"
    if not review_id:
        raise ValueError(f"{label}: blank review_id")
    if status != "REVIEWED":
        raise ValueError(f"{label}: only evidence-backed REVIEWED records belong in the review registry")
    if not row["evidence_ref"].strip():
        raise ValueError(f"{label}: evidence_ref required")
    if row["trigger_type"].strip().upper() in FORBIDDEN_TRIGGERS:
        raise ValueError(f"{label}: turn/round trigger forbidden")
    for field in NUMERIC_FIELDS:
        int_field(row, field, label)
    if not row["target_selector"].strip():
        raise ValueError(f"{label}: target_selector required")
    if row["effect_profile_id"].strip() not in techniques:
        raise ValueError(f"{label}: effect_profile_id must reference the technique catalog")

    if skill_type == "BASE":
        source = base_by_key.get((hero_id, skill_key))
        if source is None:
            raise ValueError(f"{label}: review references unknown base skill")
        compare_fields = (
            "ability_kind", "trigger_type", "rage_cost", "rage_gain", "cooldown_ms", "cast_time_ms",
            "impact_ms", "recovery_ms", "target_selector", "effect_profile_id", "special_mechanic",
        )
        mismatch = [field for field in compare_fields if (row.get(field) or "").strip() != (source.get(field) or "").strip()]
        if mismatch:
            raise ValueError(f"{label}: review no longer matches current production candidate fields {mismatch}")
    elif skill_type == "AWAKENING":
        if (hero_id, skill_key) not in awakening_keys:
            raise ValueError(f"{label}: review references unknown Awakening Skill")
        if row["ability_kind"].strip() != "AWAKENING_SKILL":
            raise ValueError(f"{label}: Awakening review must use AWAKENING_SKILL")
    else:
        raise ValueError(f"{label}: skill_type must be BASE or AWAKENING")
    return skill_type, review_id


def main() -> int:
    parser = argparse.ArgumentParser(); parser.add_argument("--enforce", action="store_true"); args = parser.parse_args()
    try:
        subprocess.run([sys.executable, str(EFFECT_VALIDATOR)], cwd=ROOT, check=True)
        with tempfile.TemporaryDirectory(prefix="m59-mechanics-") as tmp:
            candidate_path = Path(tmp) / "candidate.csv"
            subprocess.run([sys.executable, str(GENERATOR), "--output", str(candidate_path)], cwd=ROOT, check=True)
            subprocess.run([sys.executable, str(M50_VALIDATOR), "--candidate", str(candidate_path)], cwd=ROOT, check=True)
            candidate = rows(candidate_path)
        awakenings = rows(AWAKENINGS)
        techniques = technique_ids()
        if len(candidate) != EXPECTED_BASE:
            raise ValueError(f"expected {EXPECTED_BASE} base skills, found {len(candidate)}")
        if len({row['hero_id'].strip() for row in candidate}) != EXPECTED_HEROES:
            raise ValueError(f"expected {EXPECTED_HEROES} Hero Versions")
        if len(awakenings) != EXPECTED_AWAKENINGS:
            raise ValueError(f"expected {EXPECTED_AWAKENINGS} Awakening Skills, found {len(awakenings)}")

        base_by_key: dict[tuple[str, str], dict[str, str]] = {}
        for row in candidate:
            key = (row["hero_id"].strip(), row["slot"].strip())
            if key in base_by_key:
                raise ValueError(f"duplicate base mechanics key {key}")
            base_by_key[key] = row
            validate_base_mechanics(row, techniques)

        awakening_keys = {(row["hero_id"].strip(), row["awakening_id"].strip()) for row in awakenings}
        if len(awakening_keys) != EXPECTED_AWAKENINGS:
            raise ValueError("Awakening mechanics identity keys are not unique")

        review_rows = rows(REVIEWS)
        review_ids: set[str] = set(); reviewed_base: set[tuple[str, str]] = set(); reviewed_awakenings: set[tuple[str, str]] = set()
        for row in review_rows:
            skill_type, review_id = validate_review(row, base_by_key, awakening_keys, techniques)
            if review_id in review_ids:
                raise ValueError(f"duplicate mechanics review_id {review_id}")
            review_ids.add(review_id)
            key = (row["hero_id"].strip(), row["skill_key"].strip())
            target = reviewed_base if skill_type == "BASE" else reviewed_awakenings
            if key in target:
                raise ValueError(f"duplicate mechanics review key {skill_type}:{key}")
            target.add(key)

        print(
            f"M59_MECHANICS_AUDIT heroes={EXPECTED_HEROES} base={EXPECTED_BASE} structurally_realtime={len(base_by_key)} "
            f"reviewed_base={len(reviewed_base)} awakenings={EXPECTED_AWAKENINGS} reviewed_awakenings={len(reviewed_awakenings)} "
            f"review_records={len(review_rows)}"
        )
        if args.enforce and (len(reviewed_base) != EXPECTED_BASE or len(reviewed_awakenings) != EXPECTED_AWAKENINGS):
            print(
                f"M59_MECHANICS_BLOCKED reviewed_base={len(reviewed_base)}/{EXPECTED_BASE} "
                f"reviewed_awakenings={len(reviewed_awakenings)}/{EXPECTED_AWAKENINGS}", file=sys.stderr,
            )
            return 1
        print("M59_MECHANICS_OK realtime_ms=1 rage=1 truthful_review_state=1")
        return 0
    except (ValueError, subprocess.CalledProcessError) as error:
        print(f"M59_MECHANICS_INVALID {error}", file=sys.stderr); return 1


if __name__ == "__main__":
    raise SystemExit(main())
