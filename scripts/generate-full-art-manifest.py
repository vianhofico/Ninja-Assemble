#!/usr/bin/env python3
"""Generate a complete art-manifest skeleton from reference + expanded variant census.

Existing artist-authored rows are preserved verbatim by (character_id, variant), including
review status and any hand-tuned Addressables paths. Missing rows are added as TODO.
"""
from __future__ import annotations

import argparse
import csv
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "game-data/reference/variant-census.csv"
EXPANDED = ROOT / "game-data/reference/variant-census-expanded.csv"
DEFAULT_MANIFEST = ROOT / "art/manifests/hero-art-manifest.csv"
FIELDS = [
    "character_id", "variant", "portrait_address", "icon_address", "prefab_address",
    "animation_set", "vfx_set", "sfx_set", "status"
]


def slug(value: str) -> str:
    ascii_value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]+", "-", ascii_value.lower()).strip("-") or "base"


def load_variants() -> list[tuple[str, str]]:
    result: set[tuple[str, str]] = set()
    for path in (REFERENCE, EXPANDED):
        with path.open(encoding="utf-8", newline="") as handle:
            for row in csv.DictReader(handle):
                character_id = row["character_id"].strip()
                variant = row["variant"].strip()
                if character_id and variant:
                    result.add((character_id, variant))
    return sorted(result, key=lambda item: (item[0], item[1].casefold()))


def load_existing(path: Path) -> dict[tuple[str, str], dict[str, str]]:
    if not path.exists():
        return {}
    with path.open(encoding="utf-8", newline="") as handle:
        return {
            (row["character_id"].strip(), row["variant"].strip()): row
            for row in csv.DictReader(handle)
        }


def new_row(character_id: str, variant: str) -> dict[str, str]:
    variant_slug = slug(variant)
    base = f"heroes/{character_id}/{variant_slug}"
    return {
        "character_id": character_id,
        "variant": variant,
        "portrait_address": f"{base}/portrait",
        "icon_address": f"{base}/icon",
        "prefab_address": f"{base}/prefab",
        "animation_set": f"animations/{character_id}/{variant_slug}",
        "vfx_set": f"vfx/{character_id}/{variant_slug}",
        "sfx_set": f"sfx/{character_id}/{variant_slug}",
        "status": "TODO",
    }


def build_rows(manifest: Path) -> list[dict[str, str]]:
    existing = load_existing(manifest)
    return [existing.get(key, new_row(*key)) for key in load_variants()]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--check", action="store_true", help="fail unless manifest covers every census variant")
    args = parser.parse_args()

    variants = load_variants()
    existing = load_existing(args.manifest)
    missing = [key for key in variants if key not in existing]

    if args.check:
        if missing:
            print(f"ART_MANIFEST_INCOMPLETE missing={len(missing)} total={len(variants)}")
            return 1
        print(f"ART_MANIFEST_COMPLETE rows={len(variants)}")
        return 0

    output = args.output or args.manifest
    rows = build_rows(args.manifest)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    ready = sum(row["status"].strip().upper() == "READY" for row in rows)
    print(f"WROTE {output} rows={len(rows)} ready={ready}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
