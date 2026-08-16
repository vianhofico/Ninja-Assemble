#!/usr/bin/env python3
"""Validate evidence-backed balance/reference profile confidence.

A profile can only be marked VERIFIED when its measurement corpus satisfies the
minimum sample, context and evidence-reference thresholds declared in
`game-data/reference/balance-profiles.csv`.

This validator intentionally accepts empty datasets for EXPERIMENTAL profiles.
It never invents observations or upgrades confidence automatically.
"""
from __future__ import annotations

import csv
from collections import defaultdict
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
PROFILE_FILE = ROOT / "game-data/reference/balance-profiles.csv"
MEASUREMENT_GLOB = "game-data/reference/measurements/*.csv"
VALID_STATUS = {"EXPERIMENTAL", "OBSERVED", "VERIFIED"}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def fail(message: str) -> None:
    raise ValueError(message)


def positive_int(row: dict[str, str], key: str, profile_id: str) -> int:
    try:
        value = int(row[key])
    except (KeyError, ValueError):
        fail(f"{profile_id}: {key} must be an integer")
    if value < 0:
        fail(f"{profile_id}: {key} must be >= 0")
    return value


def main() -> int:
    try:
        profiles = rows(PROFILE_FILE)
        if not profiles:
            fail("balance profile registry must not be empty")

        profile_by_id: dict[str, dict[str, str]] = {}
        for row in profiles:
            profile_id = row.get("profile_id", "").strip()
            if not profile_id:
                fail("balance profile has blank profile_id")
            if profile_id in profile_by_id:
                fail(f"duplicate balance profile_id: {profile_id}")
            status = row.get("status", "").strip().upper()
            if status not in VALID_STATUS:
                fail(f"{profile_id}: invalid status {status!r}")
            positive_int(row, "min_samples", profile_id)
            positive_int(row, "min_distinct_contexts", profile_id)
            positive_int(row, "min_evidence_refs", profile_id)
            profile_by_id[profile_id] = row

        samples: dict[str, list[dict[str, str]]] = defaultdict(list)
        measurement_ids: set[str] = set()
        measurement_files = sorted(ROOT.glob(MEASUREMENT_GLOB))
        if not measurement_files:
            fail("no reference measurement templates found")

        for path in measurement_files:
            for row in rows(path):
                measurement_id = row.get("measurement_id", "").strip()
                profile_id = row.get("profile_id", "").strip()
                context_key = row.get("context_key", "").strip()
                evidence_ref = row.get("evidence_ref", "").strip()
                if not measurement_id:
                    fail(f"{path.name}: blank measurement_id")
                if measurement_id in measurement_ids:
                    fail(f"duplicate measurement_id: {measurement_id}")
                measurement_ids.add(measurement_id)
                if profile_id not in profile_by_id:
                    fail(f"{measurement_id}: unknown profile_id {profile_id!r}")
                if not context_key:
                    fail(f"{measurement_id}: context_key is required")
                if not evidence_ref:
                    fail(f"{measurement_id}: evidence_ref is required")
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

            if status == "VERIFIED":
                required_samples = positive_int(profile, "min_samples", profile_id)
                required_contexts = positive_int(profile, "min_distinct_contexts", profile_id)
                required_refs = positive_int(profile, "min_evidence_refs", profile_id)
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
                f"REFERENCE_PROFILE {profile_id} status={status} "
                f"samples={len(profile_samples)} contexts={len(contexts)} evidence_refs={len(evidence_refs)}"
            )

        print(
            f"REFERENCE_EVIDENCE_OK profiles={len(profile_by_id)} "
            f"verified={verified} observed={observed} measurements={len(measurement_ids)}"
        )
        return 0
    except ValueError as error:
        print(f"REFERENCE_EVIDENCE_INVALID {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
