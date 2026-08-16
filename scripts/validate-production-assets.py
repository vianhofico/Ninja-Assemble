#!/usr/bin/env python3
"""Require concrete repo files behind art component states that claim READY.

Package descriptor convention:
  art/packages/<character_id>/<variant-slug>/package.json

Development builds may have TODO/CONCEPT rows without package descriptors. Any component
marked READY must be backed by a descriptor and an existing repo-relative file path.
"""
from __future__ import annotations

import csv
import json
import re
import unicodedata
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
STATUS_FILE = ROOT / "art/manifests/hero-art-component-status.csv"
COMPONENT_TO_PATH = {
    "portrait_status": "portrait",
    "icon_status": "icon",
    "chibi_prefab_status": "chibiPrefab",
    "animation_status": "animationSet",
    "vfx_status": "vfxSet",
    "sfx_status": "sfxSet",
    "regression_capture_status": "regressionCapture",
}


def slug(value: str) -> str:
    normalized = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]+", "-", normalized.lower()).strip("-") or "base"


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def package_path(character_id: str, variant: str) -> Path:
    return ROOT / "art/packages" / character_id / slug(variant) / "package.json"


def resolve_repo_path(value: str, label: str, errors: list[str]) -> Path | None:
    if not value.strip():
        errors.append(f"{label}: blank path")
        return None
    path = (ROOT / value).resolve()
    try:
        path.relative_to(ROOT.resolve())
    except ValueError:
        errors.append(f"{label}: path escapes repository: {value}")
        return None
    return path


def main() -> int:
    errors: list[str] = []
    checked_files = 0
    package_count = 0

    for row in rows(STATUS_FILE):
        character_id = row["character_id"].strip()
        variant = row["variant"].strip()
        key = f"{character_id}::{variant}"
        ready_components = [field for field in COMPONENT_TO_PATH if row.get(field, "").strip().upper() == "READY"]
        review_ready = row.get("review_status", "").strip().upper() == "READY"
        if not ready_components and not review_ready:
            continue

        descriptor_path = package_path(character_id, variant)
        if not descriptor_path.exists():
            errors.append(f"{key}: READY component requires descriptor {descriptor_path.relative_to(ROOT)}")
            continue

        package_count += 1
        try:
            descriptor = json.loads(descriptor_path.read_text(encoding="utf-8"))
        except Exception as exc:
            errors.append(f"{key}: invalid package descriptor: {exc}")
            continue

        if descriptor.get("characterId") != character_id or descriptor.get("variant") != variant:
            errors.append(f"{key}: descriptor identity mismatch")

        paths = descriptor.get("paths") or {}
        for status_field in ready_components:
            path_field = COMPONENT_TO_PATH[status_field]
            repo_path = resolve_repo_path(str(paths.get(path_field, "")), f"{key} {path_field}", errors)
            if repo_path is not None:
                checked_files += 1
                if not repo_path.exists():
                    errors.append(f"{key}: READY {path_field} file does not exist: {repo_path.relative_to(ROOT)}")

        if review_ready:
            evidence = descriptor.get("reviewEvidence") or []
            if not evidence:
                errors.append(f"{key}: review_status READY requires reviewEvidence")
            for value in evidence:
                repo_path = resolve_repo_path(str(value), f"{key} reviewEvidence", errors)
                if repo_path is not None:
                    checked_files += 1
                    if not repo_path.exists():
                        errors.append(f"{key}: review evidence does not exist: {repo_path.relative_to(ROOT)}")

    if errors:
        print("PRODUCTION_ASSET_VALIDATION_FAILED", file=sys.stderr)
        for error in errors:
            print(" -", error, file=sys.stderr)
        return 1

    print(f"PRODUCTION_ASSET_VALIDATION_OK packages={package_count} checked_files={checked_files}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
