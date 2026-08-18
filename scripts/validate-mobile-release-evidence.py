#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "game-data/release/mobile-device-evidence.csv"
PROFILES = ROOT / "game-data/release/m76-device-profiles.json"
ALLOWED_CLASSES = {"LOW", "MID", "HIGH"}
TRUTHY = {"true", "1", "yes"}


def as_bool(value: str) -> bool:
    return value.strip().lower() in TRUTHY


def number(value: str, label: str, errors: list[str]) -> float | None:
    try:
        parsed = float(value)
    except (TypeError, ValueError):
        errors.append(f"{label} must be numeric")
        return None
    if parsed <= 0:
        errors.append(f"{label} must be > 0")
        return None
    return parsed


def current_commit() -> str:
    try:
        return subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    except Exception:
        return ""


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true")
    args = parser.parse_args()
    errors: list[str] = []
    profile_doc = json.loads(PROFILES.read_text(encoding="utf-8"))
    profile_version = profile_doc.get("profileVersion", "")
    class_profiles = profile_doc.get("classes", {})
    release_rule = profile_doc.get("releaseRule", {})
    if set(class_profiles) != ALLOWED_CLASSES:
        errors.append("M76 device profiles must define LOW/MID/HIGH exactly")

    with EVIDENCE.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))

    ids: set[str] = set()
    passing: list[dict[str, str]] = []
    head = current_commit()
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
            continue
        git_sha = row.get("git_sha", "").strip().lower()
        if not re.fullmatch(r"[0-9a-f]{40}", git_sha):
            errors.append(f"{evidence_id}: git_sha must be a full 40-character SHA")
        for required in ("unity_version", "artifact_type", "artifact_ref", "device_model", "android_version", "build_fingerprint", "capture_ref"):
            if not row.get(required, "").strip():
                errors.append(f"{evidence_id}: missing {required}")
        if row.get("artifact_type", "").strip().upper() not in {"APK", "AAB"}:
            errors.append(f"{evidence_id}: artifact_type must be APK or AAB")
        if row.get("benchmark_profile", "").strip() != profile_version:
            errors.append(f"{evidence_id}: benchmark_profile must be {profile_version}")
        physical = as_bool(row.get("physical_device", ""))
        if not physical:
            errors.append(f"{evidence_id}: physical_device must be true; emulator evidence cannot certify release")

        smoke = as_bool(row.get("smoke_pass", ""))
        performance = as_bool(row.get("performance_pass", ""))
        avg = number(row.get("avg_fps", ""), f"{evidence_id} avg_fps", errors) if performance else None
        p95 = number(row.get("p95_frame_ms", ""), f"{evidence_id} p95_frame_ms", errors) if performance else None
        memory = number(row.get("max_memory_mb", ""), f"{evidence_id} max_memory_mb", errors) if performance else None
        thresholds = class_profiles[device_class]
        threshold_pass = all(value is not None for value in (avg, p95, memory))
        if threshold_pass:
            if avg < float(thresholds["minAverageFps"]):
                errors.append(f"{evidence_id}: avg_fps {avg} below {device_class} target {thresholds['minAverageFps']}")
                threshold_pass = False
            if p95 > float(thresholds["maxP95FrameMs"]):
                errors.append(f"{evidence_id}: p95_frame_ms {p95} above {device_class} target {thresholds['maxP95FrameMs']}")
                threshold_pass = False
            if memory > float(thresholds["maxMemoryMb"]):
                errors.append(f"{evidence_id}: max_memory_mb {memory} above {device_class} target {thresholds['maxMemoryMb']}")
                threshold_pass = False
        if smoke and performance and physical and threshold_pass:
            passing.append(row)

    if args.release:
        required_rows = int(release_rule.get("minimumPassingPhysicalDevices", 2))
        required_models = int(release_rule.get("minimumDistinctDeviceModels", 2))
        required_classes = int(release_rule.get("minimumDistinctClasses", 2))
        distinct_devices = {row["device_model"].strip() for row in passing}
        distinct_classes = {row["device_class"].strip().upper() for row in passing}
        if len(passing) < required_rows:
            errors.append(f"release requires >={required_rows} passing physical-device rows, has {len(passing)}")
        if len(distinct_devices) < required_models:
            errors.append(f"release requires >={required_models} distinct Android device models, has {len(distinct_devices)}")
        if len(distinct_classes) < required_classes:
            errors.append(f"release requires >={required_classes} device classes among LOW/MID/HIGH, has {len(distinct_classes)}")
        if release_rule.get("requireExactCommitSha") is True and head:
            mismatched = [row["evidence_id"] for row in passing if row["git_sha"].strip().lower() != head.lower()]
            if mismatched:
                errors.append(f"release device evidence must match exact commit {head}: {mismatched}")

    if errors:
        print("MOBILE_DEVICE_EVIDENCE_INVALID", file=sys.stderr)
        for error in errors:
            print(" -", error, file=sys.stderr)
        return 1

    print(
        f"MOBILE_DEVICE_EVIDENCE_OK rows={len(rows)} passing={len(passing)} "
        f"profile={profile_version} mode={'release' if args.release else 'development'}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
