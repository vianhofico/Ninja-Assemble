#!/usr/bin/env python3
"""Generate the Unity runtime art catalog from the reviewed art manifest.

Only rows that exist in the human-managed manifest are emitted. Runtime code may derive a
convention address for untracked variants, but it never treats those derived addresses as READY.
"""
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "art/manifests/hero-art-manifest.csv"
DEFAULT_OUTPUT = ROOT / "client-unity/Assets/Resources/Generated/hero-art-runtime-catalog.json"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    with MANIFEST.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.DictReader(handle))

    entries = []
    seen = set()
    for row in rows:
        key = (row["character_id"].strip(), row["variant"].strip())
        if key in seen:
            raise SystemExit(f"duplicate hero art manifest identity: {key}")
        seen.add(key)
        entries.append({
            "characterId": key[0],
            "variant": key[1],
            "portraitAddress": row["portrait_address"].strip(),
            "iconAddress": row["icon_address"].strip(),
            "prefabAddress": row["prefab_address"].strip(),
            "animationSet": row["animation_set"].strip(),
            "vfxSet": row["vfx_set"].strip(),
            "sfxSet": row["sfx_set"].strip(),
            "status": row["status"].strip().upper(),
        })

    entries.sort(key=lambda item: (item["characterId"], item["variant"].casefold()))
    payload = {"entries": entries}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"UNITY_ART_RUNTIME_CATALOG_OK rows={len(entries)} output={args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
