#!/usr/bin/env python3
import argparse
import csv
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


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
    localizations = many("game-data/localization/*.csv")
    ready_art = [row for row in art if row.get("status", "").strip() == "READY"]
    concept_art = [row for row in art if row.get("status", "").strip() == "CONCEPT"]
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
        "releaseReady": len(roster) >= 180 and len(variants) >= 300 and len(techniques) >= 100
                        and len(kits) >= 35 and len({row["character_id"] for row in maps}) == len(roster)
                        and len(art) == len(variants) and len(ready_art) == len(variants)
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
        "", "The final two art gates intentionally remain blocked until every playable variant has a reviewed portrait/icon/chibi prefab/animation/VFX/SFX package."
    ]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--markdown", action="store_true")
    parser.add_argument("--enforce", action="store_true")
    args = parser.parse_args()
    data = audit()
    if args.json: print(json.dumps(data, indent=2, ensure_ascii=False))
    else: print(markdown(data) if args.markdown or not args.json else "")
    return 0 if (data["releaseReady"] or not args.enforce) else 1


if __name__ == "__main__":
    sys.exit(main())
