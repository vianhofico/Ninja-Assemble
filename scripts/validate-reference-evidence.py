#!/usr/bin/env python3
"""Validate evidence-backed reference/balance confidence and canonical realtime schemas.

M57 makes the evidence registry a production gate without inventing observations.
EXPERIMENTAL profiles may have empty corpora, while OBSERVED/VERIFIED profiles must
be backed by concrete measurement rows. VERIFIED additionally has to satisfy the
sample/context/evidence thresholds declared in balance-profiles.csv.
"""
from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
PROFILE_FILE = ROOT / "game-data/reference/balance-profiles.csv"
MEASUREMENT_DIR = ROOT / "game-data/reference/measurements"
VALID_STATUS = {"EXPERIMENTAL", "OBSERVED", "VERIFIED"}
PROMOTION_DEBT = {"UNKNOWN", "INFERRED", "RESEARCH_REQUIRED", "TODO", "PLACEHOLDER"}
LEGACY_COLUMNS = {"energy_before", "energy_after", "duration_turns", "cooldown_turns", "cast_turns", "recovery_turns"}
COMMON_COLUMNS = {"measurement_id", "profile_id", "context_key", "evidence_ref", "observed_at", "notes"}

SCHEMAS: dict[str, tuple[str, set[str]]] = {
    "COMBAT_STATS": ("combat-stats.csv", COMMON_COLUMNS | {
        "character_id", "variant", "level", "hp", "physical_attack", "chakra_attack",
        "physical_defense", "chakra_defense", "speed", "critical_rate",
    }),
    "DAMAGE_FORMULA": ("battle-damage.csv", COMMON_COLUMNS | {
        "attacker_id", "attacker_level", "defender_id", "defender_level", "damage_channel",
        "is_critical", "observed_damage",
    }),
    "SUMMON_PROFILE": ("summon-samples.csv", COMMON_COLUMNS | {
        "banner_id", "pull_index", "observed_rarity", "hero_id", "pity_counter_before", "pity_triggered",
    }),
    "LEVEL_COST": ("level-cost.csv", COMMON_COLUMNS | {
        "character_id", "variant", "from_level", "to_level", "observed_gold_cost", "other_costs",
    }),
    "ABILITY_CYCLE": ("ability-cycle.csv", COMMON_COLUMNS | {
        "character_id", "variant", "ability_id", "ability_kind", "rage_before", "rage_after",
        "coefficient_bps", "cooldown_ms", "cast_time_ms", "recovery_ms",
    }),
    "STRUCTURED_EFFECTS": ("structured-effects.csv", COMMON_COLUMNS | {
        "ability_id", "effect_index", "effect_type", "target_selector", "status_id",
        "duration_ms", "tick_interval_ms", "expected_amount", "observed_amount",
    }),
    "TECHNIQUE_MAPPING": ("technique-mapping.csv", COMMON_COLUMNS | {
        "technique_id", "character_id", "variant", "effect_summary", "mapping_basis",
    }),
    "PASSIVE_LIFECYCLE": ("passive-lifecycle.csv", COMMON_COLUMNS | {
        "passive_id", "trigger_id", "character_id", "variant", "expected_effect", "observed_effect",
    }),
    "REALTIME_TIMING": ("realtime-timing.csv", COMMON_COLUMNS | {
        "character_id", "variant", "ability_id", "event_type", "attack_interval_ms", "cast_time_ms",
        "recovery_ms", "cooldown_ms", "battle_elapsed_ms",
    }),
    "RAGE_RULES": ("rage-rules.csv", COMMON_COLUMNS | {
        "character_id", "variant", "ability_id", "event_type", "rage_before", "rage_delta",
        "rage_after", "rage_cost", "ready_before", "ready_after",
    }),
}


def read_table(path: Path) -> tuple[list[str], list[dict[str, str]]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        return list(reader.fieldnames or []), list(reader)


def fail(message: str) -> None:
    raise ValueError(message)


def required_positive_int(row: dict[str, str], key: str, profile_id: str) -> int:
    try:
        value = int(row.get(key, ""))
    except ValueError:
        fail(f"{profile_id}: {key} must be an integer")
    if value <= 0:
        fail(f"{profile_id}: {key} must be > 0")
    return value


def debt_token(value: str) -> str | None:
    upper = (value or "").strip().upper()
    for token in PROMOTION_DEBT:
        if token in upper:
            return token
    return None


def validate_schema(path: Path, category: str, required: set[str]) -> tuple[list[str], list[dict[str, str]]]:
    if not path.exists():
        fail(f"{category}: missing measurement corpus {path.name}")
    fields, data = read_table(path)
    field_set = set(fields)
    missing = sorted(required - field_set)
    if missing:
        fail(f"{path.name}: missing required columns {missing}")
    legacy = sorted(field_set & LEGACY_COLUMNS)
    legacy += sorted(column for column in field_set if column.endswith("_turns") and column not in legacy)
    if legacy:
        fail(f"{path.name}: legacy turn/energy columns are forbidden: {sorted(set(legacy))}")
    return fields, data


def main() -> int:
    try:
        profile_fields, profiles = read_table(PROFILE_FILE)
        if not profiles:
            fail("balance profile registry must not be empty")
        for required in ("profile_id", "category", "status", "min_samples", "min_distinct_contexts", "min_evidence_refs"):
            if required not in profile_fields:
                fail(f"balance-profiles.csv missing column {required}")

        profile_by_id: dict[str, dict[str, str]] = {}
        categories_present: set[str] = set()
        for row in profiles:
            profile_id = row.get("profile_id", "").strip()
            category = row.get("category", "").strip().upper()
            status = row.get("status", "").strip().upper()
            if not profile_id:
                fail("balance profile has blank profile_id")
            if profile_id in profile_by_id:
                fail(f"duplicate balance profile_id: {profile_id}")
            if category not in SCHEMAS:
                fail(f"{profile_id}: unsupported release category {category!r}")
            if status not in VALID_STATUS:
                fail(f"{profile_id}: invalid status {status!r}")
            required_positive_int(row, "min_samples", profile_id)
            required_positive_int(row, "min_distinct_contexts", profile_id)
            required_positive_int(row, "min_evidence_refs", profile_id)
            if status in {"OBSERVED", "VERIFIED"}:
                for key, value in row.items():
                    token = debt_token(value or "")
                    if token:
                        fail(f"{profile_id}: promoted profile still contains {token} in {key}")
            profile_by_id[profile_id] = row
            categories_present.add(category)

        missing_categories = sorted(set(SCHEMAS) - categories_present)
        if missing_categories:
            fail(f"release profile registry missing categories: {missing_categories}")

        expected_files = {filename for filename, _ in SCHEMAS.values()}
        actual_files = {path.name for path in MEASUREMENT_DIR.glob("*.csv")}
        unknown_files = sorted(actual_files - expected_files)
        missing_files = sorted(expected_files - actual_files)
        if unknown_files:
            fail(f"unregistered measurement corpus files: {unknown_files}")
        if missing_files:
            fail(f"missing measurement corpus files: {missing_files}")

        samples: dict[str, list[dict[str, str]]] = defaultdict(list)
        measurement_ids: set[str] = set()
        file_counts: dict[str, int] = {}

        for category, (filename, required) in SCHEMAS.items():
            path = MEASUREMENT_DIR / filename
            _, data = validate_schema(path, category, required)
            file_counts[filename] = len(data)
            for row in data:
                measurement_id = row.get("measurement_id", "").strip()
                profile_id = row.get("profile_id", "").strip()
                context_key = row.get("context_key", "").strip()
                evidence_ref = row.get("evidence_ref", "").strip()
                observed_at = row.get("observed_at", "").strip()
                if not measurement_id:
                    fail(f"{filename}: blank measurement_id")
                if measurement_id in measurement_ids:
                    fail(f"duplicate measurement_id: {measurement_id}")
                measurement_ids.add(measurement_id)
                profile = profile_by_id.get(profile_id)
                if profile is None:
                    fail(f"{measurement_id}: unknown profile_id {profile_id!r}")
                if profile.get("category", "").strip().upper() != category:
                    fail(f"{measurement_id}: profile {profile_id} belongs to {profile.get('category')}, not {category}")
                if not context_key:
                    fail(f"{measurement_id}: context_key is required")
                if not evidence_ref:
                    fail(f"{measurement_id}: evidence_ref is required")
                if not observed_at:
                    fail(f"{measurement_id}: observed_at is required")
                token = debt_token(evidence_ref)
                if token:
                    fail(f"{measurement_id}: evidence_ref contains prohibited placeholder {token}")
                samples[profile_id].append(row)

        verified = 0
        observed = 0
        for profile_id, profile in profile_by_id.items():
            status = profile["status"].strip().upper()
            profile_samples = samples.get(profile_id, [])
            contexts = {row["context_key"].strip() for row in profile_samples}
            evidence_refs = {row["evidence_ref"].strip() for row in profile_samples}

            if status == "OBSERVED" and not profile_samples:
                fail(f"{profile_id}: OBSERVED requires at least one measurement")

            if status in {"OBSERVED", "VERIFIED"}:
                for row in profile_samples:
                    for key, value in row.items():
                        token = debt_token(value or "")
                        if token:
                            fail(f"{row['measurement_id']}: {status} evidence contains {token} in {key}")

            if status == "VERIFIED":
                required_samples = required_positive_int(profile, "min_samples", profile_id)
                required_contexts = required_positive_int(profile, "min_distinct_contexts", profile_id)
                required_refs = required_positive_int(profile, "min_evidence_refs", profile_id)
                if len(profile_samples) < required_samples:
                    fail(f"{profile_id}: VERIFIED needs {required_samples} samples, has {len(profile_samples)}")
                if len(contexts) < required_contexts:
                    fail(f"{profile_id}: VERIFIED needs {required_contexts} distinct contexts, has {len(contexts)}")
                if len(evidence_refs) < required_refs:
                    fail(f"{profile_id}: VERIFIED needs {required_refs} evidence refs, has {len(evidence_refs)}")
                verified += 1
            elif status == "OBSERVED":
                observed += 1

            print(
                f"REFERENCE_PROFILE {profile_id} category={profile['category'].strip().upper()} status={status} "
                f"samples={len(profile_samples)} contexts={len(contexts)} evidence_refs={len(evidence_refs)}"
            )

        print(
            f"REFERENCE_EVIDENCE_OK profiles={len(profile_by_id)} categories={len(categories_present)} "
            f"corpora={len(file_counts)} verified={verified} observed={observed} measurements={len(measurement_ids)} "
            "legacy_turn_energy=0"
        )
        return 0
    except ValueError as error:
        print(f"REFERENCE_EVIDENCE_INVALID {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
