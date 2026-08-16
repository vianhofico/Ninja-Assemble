#!/usr/bin/env python3
import argparse
import csv
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as fh:
        return list(csv.DictReader(fh))


def read_many(pattern):
    rows = []
    files = sorted(ROOT.glob(pattern))
    for path in files:
        rows.extend(read_csv(path))
    return rows, files


def require(condition, message, errors):
    if not condition:
        errors.append(message)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--release", action="store_true", help="enable strict mobile-release gates")
    args = parser.parse_args()
    errors = []

    roster = read_csv(ROOT / "game-data/reference/roster-complete.csv")
    variants, variant_files = read_many("game-data/reference/variant-census*.csv")
    strings, localization_files = read_many("game-data/localization/*.csv")
    art = read_csv(ROOT / "art/manifests/hero-art-manifest.csv")
    techniques, technique_files = read_many("game-data/skills/technique-library*.csv")
    kits = read_csv(ROOT / "game-data/skills/kit-profiles.csv")
    character_kits = read_csv(ROOT / "game-data/skills/character-kit-map.csv")
    overrides = read_csv(ROOT / "game-data/skills/variant-kit-overrides.csv")

    roster_ids = [row["id"].strip() for row in roster]
    roster_id_set = set(roster_ids)
    require(len(roster_ids) >= 180, f"base roster below target: {len(roster_ids)} < 180", errors)
    require(len(roster_ids) == len(roster_id_set), "duplicate base roster id", errors)

    variant_keys = []
    for row in variants:
        key = (row["character_id"].strip(), row["variant"].strip())
        variant_keys.append(key)
        require(key[0] in roster_id_set, f"variant references missing character: {key}", errors)
    variant_key_set = set(variant_keys)
    require(len(variant_keys) >= 300, f"variant census below Complete Roster+ target: {len(variant_keys)} < 300", errors)
    require(len(variant_keys) == len(variant_key_set), "duplicate variant census row", errors)

    localization_keys = []
    for row in strings:
        key = row["key"].strip()
        localization_keys.append(key)
        require(bool(row["en"].strip()), f"missing English text: {key}", errors)
        require(bool(row["vi"].strip()), f"missing Vietnamese text: {key}", errors)
    require(len(localization_keys) == len(set(localization_keys)), "duplicate localization key", errors)

    technique_ids = []
    allowed_channels = {"PHYSICAL", "CHAKRA"}
    allowed_kinds = {"BASIC", "ACTIVE", "ULTIMATE", "PASSIVE"}
    for row in techniques:
        technique_id = row["technique_id"].strip()
        technique_ids.append(technique_id)
        for field in ("name_en", "name_vi", "description_en", "description_vi"):
            require(bool(row[field].strip()), f"technique {technique_id} missing {field}", errors)
        require(row["channel"].strip() in allowed_channels, f"invalid technique channel: {technique_id}", errors)
        require(row["kind"].strip() in allowed_kinds, f"invalid technique kind: {technique_id}", errors)
    technique_id_set = set(technique_ids)
    require(len(technique_ids) >= 100, f"technique library below diversity target: {len(technique_ids)} < 100", errors)
    require(len(technique_ids) == len(technique_id_set), "duplicate technique id", errors)

    kit_ids = set()
    for row in kits:
        kit_id = row["profile_id"].strip()
        require(kit_id not in kit_ids, f"duplicate kit profile: {kit_id}", errors)
        kit_ids.add(kit_id)
        for field in ("basic", "skill1", "skill2", "ultimate", "passive"):
            value = row[field].strip()
            require(value in technique_id_set, f"kit {kit_id} references missing technique {field}={value}", errors)
    require(len(kit_ids) >= 35, f"kit profile count below diversity target: {len(kit_ids)} < 35", errors)

    mapped_characters = set()
    for row in character_kits:
        character_id = row["character_id"].strip()
        profile_id = row["profile_id"].strip()
        require(character_id in roster_id_set, f"kit map references missing character: {character_id}", errors)
        require(profile_id in kit_ids, f"kit map references missing profile: {profile_id}", errors)
        require(character_id not in mapped_characters, f"duplicate character kit mapping: {character_id}", errors)
        mapped_characters.add(character_id)
    require(mapped_characters == roster_id_set, f"every base character requires a kit mapping; mapped={len(mapped_characters)} roster={len(roster_id_set)}", errors)

    override_keys = set()
    for row in overrides:
        key = (row["character_id"].strip(), row["variant"].strip())
        profile_id = row["profile_id"].strip()
        require(key in variant_key_set, f"variant kit override references missing census variant: {key}", errors)
        require(profile_id in kit_ids, f"variant kit override references missing profile: {key} -> {profile_id}", errors)
        require(key not in override_keys, f"duplicate variant kit override: {key}", errors)
        override_keys.add(key)

    allowed_art_status = {"CONCEPT", "IN_PROGRESS", "REVIEW", "READY"}
    art_by_key = {}
    for row in art:
        key = (row["character_id"].strip(), row["variant"].strip())
        require(row["status"].strip() in allowed_art_status, f"invalid art status: {key}", errors)
        require(key not in art_by_key, f"duplicate art manifest row: {key}", errors)
        require(key in variant_key_set, f"art manifest references missing census variant: {key}", errors)
        art_by_key[key] = row

    if args.release:
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
    print(f"base_characters={len(roster_ids)} variants={len(variant_keys)} localization_keys={len(localization_keys)} art_rows={len(art_by_key)} techniques={len(technique_ids)} kits={len(kit_ids)} mapped_characters={len(mapped_characters)} variant_overrides={len(override_keys)}")
    print("variant_files=" + ",".join(path.name for path in variant_files))
    print("technique_files=" + ",".join(path.name for path in technique_files))
    return 0


if __name__ == "__main__":
    sys.exit(main())
