#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "game-data/release/mobile-device-evidence.csv"
ALLOWED_CLASSES = {"LOW", "MID", "HIGH"}
TRUTHY = {"true", "1", "yes"}


def as_bool(value: str) -> bool:
    return value.strip().lower() in TRUTHY


def positive_number(value: str, label: str, errors: list[str]) -> float | None:
    try:
        parsed = float(value)
    except ValueError:
        errors.append(f"{label} must be numeric")
        return None
    if parsed <= 0:
        errors.append(f"{label} must be > 0")
    return parsed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true")
    args = parser.parse_args()
    errors: list[str] = []
    with EVIDENCE.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))

    ids: set[str] = set()
    passing: list[dict[str, str]] = []
    for row in rows:
        evidence_id = row.get("evidence_id", "").strip()
        if not evidence_id:
            errors.append("mobile device evidence has blank evidence_id")
            continue
        if evidence_id in ids:
            errors.append(f"duplicate mobile evidence_id: {evidence_id}")
        ids.add(evidence_id)

        device_class = row.get("device_class", "").strip().upper()
        if device_class not in ALLOWED_CLASSES:
            errors.append(f"{evidence_id}: invalid device_class={device_class!r}")
        for required in ("git_sha", "unity_version", "artifact_type", "artifact_ref", "device_model", "android_version", "capture_ref"):
            if not row.get(required, "").strip():
                errors.append(f"{evidence_id}: missing {required}")

        smoke = as_bool(row.get("smoke_pass", ""))
        performance = as_bool(row.get("performance_pass", ""))
        if smoke and performance:
            positive_number(row.get("avg_fps", ""), f"{evidence_id} avg_fps", errors)
            positive_number(row.get("p95_frame_ms", ""), f"{evidence_id} p95_frame_ms", errors)
            positive_number(row.get("max_memory_mb", ""), f"{evidence_id} max_memory_mb", errors)
            passing.append(row)

    if args.release:
        distinct_devices = {row["device_model"].strip() for row in passing}
        distinct_classes = {row["device_class"].strip().upper() for row in passing}
        if len(passing) < 2:
            errors.append(f"release requires >=2 passing device evidence rows, has {len(passing)}")
        if len(distinct_devices) < 2:
            errors.append(f"release requires >=2 distinct Android device models, has {len(distinct_devices)}")
        if len(distinct_classes) < 2:
            errors.append(f"release requires >=2 device classes among LOW/MID/HIGH, has {len(distinct_classes)}")

    if errors:
        print("MOBILE_DEVICE_EVIDENCE_INVALID", file=sys.stderr)
        for error in errors:
            print(" -", error, file=sys.stderr)
        return 1

    print(
        f"MOBILE_DEVICE_EVIDENCE_OK rows={len(rows)} passing={len(passing)} "
        f"mode={'release' if args.release else 'development'}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
