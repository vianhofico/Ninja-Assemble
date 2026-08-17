#!/usr/bin/env python3
"""Validate technique/effect coverage without coupling CI to resolver implementation details."""
from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIBRARIES = [ROOT / f"game-data/skills/technique-library-0{i}.csv" for i in range(1, 5)]
OVERRIDES = ROOT / "game-data/skills/technique-effects.csv"
RESOLVER = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/TechniqueEffectResolver.java"
TEST = ROOT / "server/src/test/java/com/ninjaassemble/play/domain/TechniqueEffectResolverTest.java"


def main() -> int:
    techniques: dict[str, dict[str, str]] = {}
    for path in LIBRARIES:
        with path.open(encoding="utf-8", newline="") as handle:
            reader = csv.DictReader(handle)
            required_columns = {"technique_id", "kind", "channel", "tags"}
            missing_columns = required_columns - set(reader.fieldnames or [])
            if missing_columns:
                raise SystemExit(f"{path.name} missing technique columns: {sorted(missing_columns)}")
            for row in reader:
                technique_id = row["technique_id"].strip()
                if not technique_id:
                    raise SystemExit(f"blank technique id in {path.name}")
                if technique_id in techniques:
                    raise SystemExit(f"duplicate technique id: {technique_id}")
                techniques[technique_id] = row
    if not techniques:
        raise SystemExit("technique catalog is empty")

    grouped: dict[str, list[int]] = defaultdict(list)
    with OVERRIDES.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        fields = set(reader.fieldnames or [])
        if "duration_turns" in fields:
            raise SystemExit("deprecated duration_turns column remains")
        required_effect_columns = {
            "technique_id", "effect_index", "effect_type", "target_selector", "chance_bps",
            "duration_ms", "tick_interval_ms", "profile_id", "status"
        }
        missing_columns = required_effect_columns - fields
        if missing_columns:
            raise SystemExit(f"effect table missing columns: {sorted(missing_columns)}")
        for row in reader:
            technique_id = row["technique_id"].strip()
            if technique_id not in techniques:
                raise SystemExit(f"override references unknown technique: {technique_id}")
            if not row["profile_id"].strip() or not row["status"].strip():
                raise SystemExit(f"override must retain profile/status evidence metadata: {technique_id}")
            duration_ms = int(row["duration_ms"] or 0)
            tick_interval_ms = int(row["tick_interval_ms"] or 0)
            if duration_ms < 0 or tick_interval_ms < 0:
                raise SystemExit(f"negative realtime timing for {technique_id}")
            if tick_interval_ms > 0 and duration_ms <= 0:
                raise SystemExit(f"periodic effect missing positive duration for {technique_id}")
            grouped[technique_id].append(int(row["effect_index"]))

    for technique_id, indexes in grouped.items():
        ordered = sorted(indexes)
        if ordered != list(range(1, len(ordered) + 1)):
            raise SystemExit(f"non-contiguous one-based effect indexes for {technique_id}: {ordered}")

    source = RESOLVER.read_text(encoding="utf-8")
    forbidden = ["nameEn()", "nameVi()", "descriptionEn()", "descriptionVi()", "durationTurns"]
    leaked = [token for token in forbidden if token in source]
    if leaked:
        raise SystemExit(f"resolver must not derive gameplay from prose/turn fields: {leaked}")
    for required in ("MappingStatus.RUNTIME", "MappingStatus.DEFERRED_PASSIVE", "durationMs", "tickIntervalMs"):
        if required not in source:
            raise SystemExit(f"resolver missing runtime coverage contract: {required}")

    test_source = TEST.read_text(encoding="utf-8")
    if "allTechniquesHaveAnExplicitRuntimeOrDeferredMappingState" not in test_source:
        raise SystemExit("missing Java full-catalog mapping coverage test")

    executable = sum(1 for row in techniques.values() if row["kind"] != "PASSIVE")
    passive = len(techniques) - executable
    print(
        f"TECHNIQUE_EFFECT_COVERAGE_OK techniques={len(techniques)} executable={executable} "
        f"passive_deferred={passive} curated={len(grouped)} timing=milliseconds"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
