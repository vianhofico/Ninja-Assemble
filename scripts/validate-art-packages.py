#!/usr/bin/env python3
"""Validate component-level production state for hero art packages."""
from __future__ import annotations

import argparse
import csv
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
COMPONENT_FILE = ROOT / "art/manifests/hero-art-component-status.csv"
MANIFEST_FILE = ROOT / "art/manifests/hero-art-manifest.csv"
COMPONENT_FIELDS = (
    "portrait_status", "icon_status", "chibi_prefab_status", "animation_status",
    "vfx_status", "sfx_status", "regression_capture_status", "review_status"
)
VALID = {"TODO", "CONCEPT", "IN_PROGRESS", "REVIEW", "READY"}


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def variants() -> set[tuple[str, str]]:
    out: set[tuple[str, str]] = set()
    for path in sorted(ROOT.glob("game-data/reference/variant-census*.csv")):
        for row in read(path):
            out.add((row["character_id"].strip(), row["variant"].strip()))
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true")
    args = parser.parse_args()
    errors: list[str] = []
    census = variants()
    component_rows = read(COMPONENT_FILE)
    manifest_rows = read(MANIFEST_FILE)

    components: dict[tuple[str, str], dict[str, str]] = {}
    for row in component_rows:
        key = (row["character_id"].strip(), row["variant"].strip())
        if key in components:
            errors.append(f"duplicate component package: {key}")
        if key not in census:
            errors.append(f"component package missing from variant census: {key}")
        for field in COMPONENT_FIELDS:
            value = row.get(field, "").strip().upper()
            if value not in VALID:
                errors.append(f"{key}: invalid {field}={value!r}")
        components[key] = row

    manifest = {
        (row["character_id"].strip(), row["variant"].strip()): row
        for row in manifest_rows
    }

    for key, row in manifest.items():
        if row.get("status", "").strip().upper() == "READY":
            package = components.get(key)
            if package is None:
                errors.append(f"manifest READY without component package: {key}")
                continue
            for field in COMPONENT_FIELDS:
                if package.get(field, "").strip().upper() != "READY":
                    errors.append(f"manifest READY but component not READY: {key} {field}")

    if args.release:
        for key in census:
            package = components.get(key)
            if package is None:
                errors.append(f"release missing component package: {key}")
                continue
            for field in COMPONENT_FIELDS:
                if package.get(field, "").strip().upper() != "READY":
                    errors.append(f"release component not READY: {key} {field}")

    if errors:
        print("ART PACKAGE VALIDATION FAILED", file=sys.stderr)
        for error in errors:
            print(" -", error, file=sys.stderr)
        return 1

    complete = sum(
        all(row.get(field, "").strip().upper() == "READY" for field in COMPONENT_FIELDS)
        for row in component_rows
    )
    print(f"ART_PACKAGE_OK tracked={len(component_rows)} complete={complete} census={len(census)} mode={'release' if args.release else 'development'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
