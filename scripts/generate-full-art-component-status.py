#!/usr/bin/env python3
"""Generate a 427-row component-status candidate while preserving tracked art work."""
from __future__ import annotations

import argparse
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/manifests/hero-art-component-status.csv"
FIELDS = [
    "character_id", "variant", "portrait_status", "icon_status", "chibi_prefab_status",
    "animation_status", "vfx_status", "sfx_status", "regression_capture_status", "review_status", "notes"
]


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def variant_keys() -> list[tuple[str, str]]:
    keys: set[tuple[str, str]] = set()
    for path in sorted(ROOT.glob("game-data/reference/variant-census*.csv")):
        for row in read(path):
            keys.add((row["character_id"].strip(), row["variant"].strip()))
    return sorted(keys, key=lambda item: (item[0], item[1].casefold()))


def blank(character_id: str, variant: str) -> dict[str, str]:
    return {
        "character_id": character_id,
        "variant": variant,
        "portrait_status": "TODO",
        "icon_status": "TODO",
        "chibi_prefab_status": "TODO",
        "animation_status": "TODO",
        "vfx_status": "TODO",
        "sfx_status": "TODO",
        "regression_capture_status": "TODO",
        "review_status": "TODO",
        "notes": "",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    existing = {
        (row["character_id"].strip(), row["variant"].strip()): row
        for row in read(SOURCE)
    }
    rows = [existing.get(key, blank(*key)) for key in variant_keys()]
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    print(f"FULL_ART_COMPONENT_STATUS_OK rows={len(rows)} tracked={len(existing)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
