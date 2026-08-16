#!/usr/bin/env python3
"""Validate M25 technique effect mapping coverage without deriving mechanics from prose fields."""
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
            for row in csv.DictReader(handle):
                technique_id = row["technique_id"].strip()
                if technique_id in techniques:
                    raise SystemExit(f"duplicate technique id: {technique_id}")
                techniques[technique_id] = row

    if len(techniques) != 120:
        raise SystemExit(f"expected 120 technique definitions, got {len(techniques)}")

    grouped: dict[str, list[int]] = defaultdict(list)
    with OVERRIDES.open(encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
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
        if ordered != list(range(len(ordered))):
            raise SystemExit(f"non-contiguous effect indexes for {technique_id}: {ordered}")

    source = RESOLVER.read_text(encoding="utf-8")
    forbidden = ["nameEn()", "nameVi()", "descriptionEn()", "descriptionVi()"]
    leaked = [token for token in forbidden if token in source]
    if leaked:
        raise SystemExit(f"resolver must not derive gameplay from prose fields: {leaked}")
    required = [
        "MappingStatus.RUNTIME", "MappingStatus.DEFERRED_PASSIVE", "KIND_BASIC", "TAG_HEAL_ULTIMATE",
        "TAG_BURST_ULTIMATE", "TAG_POISON", "TAG_GENJUTSU", "TAG_MIND", "TAG_KAMUI", "ACTIVE_DEFAULT"
    ]
    missing = [token for token in required if token not in source]
    if missing:
        raise SystemExit(f"resolver missing fallback coverage contracts: {missing}")

    test_source = TEST.read_text(encoding="utf-8")
    if "all120TechniquesHaveAnExplicitRuntimeOrDeferredMappingState" not in test_source:
        raise SystemExit("missing Java full-catalog mapping coverage test")

    executable = sum(1 for row in techniques.values() if row["kind"] != "PASSIVE")
    passive = len(techniques) - executable
    print(f"TECHNIQUE_EFFECT_COVERAGE_OK techniques={len(techniques)} executable={executable} passive_deferred={passive} curated={len(grouped)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
