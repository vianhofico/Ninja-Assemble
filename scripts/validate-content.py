#!/usr/bin/env python3
import argparse
import csv
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as fh:
        return list(csv.DictReader(fh))


def require(condition, message, errors):
    if not condition:
        errors.append(message)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true", help="enable strict mobile-release gates")
    args = parser.parse_args()
    errors = []

    roster = read_csv(ROOT / "game-data/reference/roster-complete.csv")
    variants = read_csv(ROOT / "game-data/reference/variant-census.csv")
    strings = read_csv(ROOT / "game-data/localization/strings.csv")
    art = read_csv(ROOT / "art/manifests/hero-art-manifest.csv")

    roster_ids = [row["id"].strip() for row in roster]
    require(len(roster_ids) >= 180, f"base roster below development target: {len(roster_ids)} < 180", errors)
    require(len(roster_ids) == len(set(roster_ids)), "duplicate base roster id", errors)

    variant_keys = []
    for row in variants:
        key = (row["character_id"].strip(), row["variant"].strip())
        variant_keys.append(key)
        require(key[0] in set(roster_ids), f"variant references missing character: {key}", errors)
    require(len(variant_keys) >= 140, f"variant census below current development floor: {len(variant_keys)} < 140", errors)
    require(len(variant_keys) == len(set(variant_keys)), "duplicate variant census row", errors)

    localization_keys = []
    for row in strings:
        key = row["key"].strip()
        localization_keys.append(key)
        require(bool(row["en"].strip()), f"missing English text: {key}", errors)
        require(bool(row["vi"].strip()), f"missing Vietnamese text: {key}", errors)
    require(len(localization_keys) == len(set(localization_keys)), "duplicate localization key", errors)

    allowed_art_status = {"CONCEPT", "IN_PROGRESS", "REVIEW", "READY"}
    art_by_key = {}
    for row in art:
        key = (row["character_id"].strip(), row["variant"].strip())
        require(row["status"].strip() in allowed_art_status, f"invalid art status: {key}", errors)
        require(key not in art_by_key, f"duplicate art manifest row: {key}", errors)
        art_by_key[key] = row

    if args.release:
        require(len(variant_keys) >= 300, f"release requires at least 300 variants; found {len(variant_keys)}", errors)
        for key in variant_keys:
            row = art_by_key.get(key)
            require(row is not None, f"release missing art manifest: {key}", errors)
            if row is not None:
                require(row["status"].strip() == "READY", f"art not READY: {key}", errors)
                for field in ("portrait_address", "icon_address", "prefab_address", "animation_set", "vfx_set", "sfx_set"):
                    require(bool(row[field].strip()), f"release art field missing {field}: {key}", errors)

    if errors:
        print("CONTENT VALIDATION FAILED")
        for error in errors:
            print(" -", error)
        return 1

    mode = "release" if args.release else "development"
    print(f"CONTENT VALIDATION PASS ({mode})")
    print(f"base_characters={len(roster_ids)} variants={len(variant_keys)} localization_keys={len(localization_keys)} art_rows={len(art_by_key)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
