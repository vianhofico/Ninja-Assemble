#!/usr/bin/env python3
import argparse
import csv
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
COMPONENT_FIELDS = (
    "portrait_status", "icon_status", "chibi_prefab_status", "animation_status",
    "vfx_status", "sfx_status", "regression_capture_status", "review_status"
)
TRUTHY = {"true", "1", "yes"}


def rows(path):
    with path.open(encoding="utf-8-sig", newline="") as fh:
        return list(csv.DictReader(fh))


def many(pattern):
    out = []
    for path in sorted(ROOT.glob(pattern)):
        out.extend(rows(path))
    return out


def audit():
    roster = rows(ROOT / "game-data/reference/roster-complete.csv")
    variants = many("game-data/reference/variant-census*.csv")
    techniques = many("game-data/skills/technique-library*.csv")
    kits = rows(ROOT / "game-data/skills/kit-profiles.csv")
    maps = rows(ROOT / "game-data/skills/character-kit-map.csv")
    overrides = rows(ROOT / "game-data/skills/variant-kit-overrides.csv")
    art = rows(ROOT / "art/manifests/hero-art-manifest.csv")
    components = rows(ROOT / "art/manifests/hero-art-component-status.csv")
    localizations = many("game-data/localization/*.csv")
    reference_profiles = rows(ROOT / "game-data/reference/balance-profiles.csv")
    mobile_evidence = rows(ROOT / "game-data/release/mobile-device-evidence.csv")

    ready_art = [row for row in art if row.get("status", "").strip() == "READY"]
    concept_art = [row for row in art if row.get("status", "").strip() == "CONCEPT"]
    complete_packages = [
        row for row in components
        if all(row.get(field, "").strip().upper() == "READY" for field in COMPONENT_FIELDS)
    ]
    verified_profiles = [row for row in reference_profiles if row.get("status", "").strip().upper() == "VERIFIED"]
    all_reference_verified = bool(reference_profiles) and len(verified_profiles) == len(reference_profiles)

    passing_devices = [
        row for row in mobile_evidence
        if row.get("smoke_pass", "").strip().lower() in TRUTHY
        and row.get("performance_pass", "").strip().lower() in TRUTHY
    ]
    distinct_models = {row.get("device_model", "").strip() for row in passing_devices if row.get("device_model", "").strip()}
    distinct_classes = {row.get("device_class", "").strip().upper() for row in passing_devices if row.get("device_class", "").strip()}
    mobile_device_gate = len(passing_devices) >= 2 and len(distinct_models) >= 2 and len(distinct_classes) >= 2

    release_ready = (
        len(roster) >= 180 and len(variants) >= 300 and len(techniques) >= 100
        and len(kits) >= 35 and len({row["character_id"] for row in maps}) == len(roster)
        and len(art) == len(variants) and len(ready_art) == len(variants)
        and len(components) == len(variants) and len(complete_packages) == len(variants)
        and all_reference_verified and mobile_device_gate
    )
    return {
        "baseCharacters": len(roster),
        "variants": len(variants),
        "techniques": len(techniques),
        "kitProfiles": len(kits),
        "mappedCharacters": len({row["character_id"] for row in maps}),
        "variantKitOverrides": len(overrides),
        "localizationKeys": len(localizations),
        "artManifestRows": len(art),
        "artReady": len(ready_art),
        "artConcept": len(concept_art),
        "artMissingManifest": max(0, len(variants) - len(art)),
        "artComponentPackagesTracked": len(components),
        "artComponentPackagesComplete": len(complete_packages),
        "artComponentPackagesMissing": max(0, len(variants) - len(components)),
        "referenceProfiles": len(reference_profiles),
        "referenceProfilesVerified": len(verified_profiles),
        "referenceProfilesAllVerified": all_reference_verified,
        "mobileDeviceEvidenceRows": len(mobile_evidence),
        "mobileDevicePassing": len(passing_devices),
        "mobileDistinctDeviceModels": len(distinct_models),
        "mobileDistinctDeviceClasses": len(distinct_classes),
        "mobileDeviceGatePass": mobile_device_gate,
        "releaseReady": release_ready,
    }


def markdown(data):
    status = "READY" if data["releaseReady"] else "NOT READY"
    lines = [
        "# Automated Release Audit", "", f"**Mobile release status: {status}**", "",
        "| Gate | Current | Target | Status |", "|---|---:|---:|---|",
        f"| Base characters | {data['baseCharacters']} | >= 180 | {'PASS' if data['baseCharacters'] >= 180 else 'BLOCKED'} |",
        f"| Variants | {data['variants']} | >= 300 | {'PASS' if data['variants'] >= 300 else 'BLOCKED'} |",
        f"| Techniques | {data['techniques']} | >= 100 | {'PASS' if data['techniques'] >= 100 else 'BLOCKED'} |",
        f"| Kit profiles | {data['kitProfiles']} | >= 35 | {'PASS' if data['kitProfiles'] >= 35 else 'BLOCKED'} |",
        f"| Base character kit mapping | {data['mappedCharacters']} | {data['baseCharacters']} | {'PASS' if data['mappedCharacters'] == data['baseCharacters'] else 'BLOCKED'} |",
        f"| Art manifest rows | {data['artManifestRows']} | {data['variants']} | {'PASS' if data['artManifestRows'] == data['variants'] else 'BLOCKED'} |",
        f"| Art READY | {data['artReady']} | {data['variants']} | {'PASS' if data['artReady'] == data['variants'] else 'BLOCKED'} |",
        f"| Component packages tracked | {data['artComponentPackagesTracked']} | {data['variants']} | {'PASS' if data['artComponentPackagesTracked'] == data['variants'] else 'BLOCKED'} |",
        f"| Component packages complete | {data['artComponentPackagesComplete']} | {data['variants']} | {'PASS' if data['artComponentPackagesComplete'] == data['variants'] else 'BLOCKED'} |",
        f"| Reference/balance profiles VERIFIED | {data['referenceProfilesVerified']} | {data['referenceProfiles']} | {'PASS' if data['referenceProfilesAllVerified'] else 'BLOCKED'} |",
        f"| Passing Android device runs | {data['mobileDevicePassing']} | >= 2 | {'PASS' if data['mobileDeviceGatePass'] else 'BLOCKED'} |",
        f"| Distinct Android device models | {data['mobileDistinctDeviceModels']} | >= 2 | {'PASS' if data['mobileDeviceGatePass'] else 'BLOCKED'} |",
        f"| Distinct device classes | {data['mobileDistinctDeviceClasses']} | >= 2 | {'PASS' if data['mobileDeviceGatePass'] else 'BLOCKED'} |",
        "",
        "A hero art package is complete only when portrait, icon, chibi prefab, animation, VFX, SFX, regression capture and final review are all READY and backed by production files.",
        "Reference/balance profiles require measured evidence; mobile device evidence requires passing smoke + performance runs on at least two distinct models/classes."
    ]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--markdown", action="store_true")
    parser.add_argument("--enforce", action="store_true")
    args = parser.parse_args()
    data = audit()
    if args.json:
        print(json.dumps(data, indent=2, ensure_ascii=False))
    else:
        print(markdown(data))
    return 0 if (data["releaseReady"] or not args.enforce) else 1


if __name__ == "__main__":
    sys.exit(main())
