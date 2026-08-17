#!/usr/bin/env python3
"""Validate full technique effect mapping coverage without deriving mechanics from prose fields."""
from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LIBRARIES = [ROOT / f"game-data/skills/technique-library-0{i}.csv" for i in range(1, 5)]
OVERRIDES = ROOT / "game-data/skills/technique-effects.csv"
RESOLVER = ROOT / "server/src/main/java/com/ninjaassemble/play/domain/TechniqueEffectResolver.java"
TEST = ROOT / "server/src/test/java/com/ninjaassemble/play/domain/TechniqueEffectResolverTest.java"
BASELINE_TECHNIQUE_COUNT = 120


def main() -> int:
    techniques: dict[str, dict[str, str]] = {}
    for path in LIBRARIES:
        with path.open(encoding="utf-8", newline="") as handle:
            for row in csv.DictReader(handle):
                technique_id = row["technique_id"].strip()
                if technique_id in techniques:
                    raise SystemExit(f"duplicate technique id: {technique_id}")
                techniques[technique_id] = row
    # The catalog is allowed to grow after M47/M50.  The coverage contract is
    # set-based, not tied forever to the original 120-technique seed.
    if len(techniques) < BASELINE_TECHNIQUE_COUNT:
        raise SystemExit(
            f"technique catalog regressed below baseline {BASELINE_TECHNIQUE_COUNT}: got {len(techniques)}"
        )

    grouped: dict[str, list[int]] = defaultdict(list)
    with OVERRIDES.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        if "duration_turns" in (reader.fieldnames or []):
            raise SystemExit("deprecated duration_turns column remains")
        for required in ("duration_ms", "tick_interval_ms"):
            if required not in (reader.fieldnames or []):
                raise SystemExit(f"missing {required}")
        for row in reader:
            technique_id = row["technique_id"].strip()
            if technique_id not in techniques:
                raise SystemExit(f"override references unknown technique: {technique_id}")
            if row["profile_id"] != "experimental-technique-mapping-v1":
                raise SystemExit(f"override uses wrong profile: {technique_id}")
            if row["status"] != "EXPERIMENTAL_RUNTIME":
                raise SystemExit(f"override must remain EXPERIMENTAL_RUNTIME: {technique_id}")
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
    required = [
        "MappingStatus.RUNTIME", "MappingStatus.DEFERRED_PASSIVE", "KIND_BASIC", "TAG_HEAL_ULTIMATE",
        "TAG_BURST_ULTIMATE", "TAG_POISON", "TAG_GENJUTSU", "TAG_MIND", "TAG_KAMUI", "ACTIVE_DEFAULT",
        "durationMs", "tickIntervalMs"
    ]
    missing = [token for token in required if token not in source]
    if missing:
        raise SystemExit(f"resolver missing coverage contracts: {missing}")

    test_source = TEST.read_text(encoding="utf-8")
    if "allTechniquesHaveAnExplicitRuntimeOrDeferredMappingState" not in test_source:
        raise SystemExit("missing Java full-catalog mapping coverage test")

    executable = sum(1 for row in techniques.values() if row["kind"] != "PASSIVE")
    passive = len(techniques) - executable
    print(
        f"TECHNIQUE_EFFECT_COVERAGE_OK techniques={len(techniques)} baseline={BASELINE_TECHNIQUE_COUNT} "
        f"executable={executable} passive_deferred={passive} curated={len(grouped)} timing=milliseconds"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
