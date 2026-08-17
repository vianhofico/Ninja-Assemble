#!/usr/bin/env python3
"""Generate the M50 five-slot skill-design candidate from the explicit M47 Hero Version aliases.

This is a structural migration, not a canon-parity claim.  It deliberately preserves provenance through
legacy_slot/source_technique_id while moving the old ULTIMATE source into the new SKILL_1 Rage/Signature slot.
Every row remains RESEARCH_REQUIRED until the per-Hero-Version canon/balance/cinematic review is completed.
"""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "game-data/skills/hero-version-skills.csv"
FINAL_SLOTS = ("BASIC", "SKILL_1", "SKILL_2", "SKILL_3", "PASSIVE")
LEGACY_REQUIRED = ("BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE")

FIELDNAMES = [
    "skill_id", "hero_id", "slot", "legacy_slot", "source_technique_id",
    "name_en", "name_vi", "channel", "ability_kind", "trigger_type", "is_signature",
    "rage_cost", "cooldown_ms", "cast_time_ms", "impact_ms", "recovery_ms",
    "cinematic_mode", "animation_key", "vfx_key", "sfx_key",
    "canon_source", "canon_confidence", "review_status", "research_note",
]


def migrate(group: dict[str, dict[str, str]]) -> list[dict[str, str]]:
    # Latest skill spec overrides the historical slot labels:
    # old ULTIMATE becomes the only signature Rage Skill (new SKILL_1);
    # the two former active skills shift to SKILL_2/SKILL_3.
    plan = [
        ("BASIC", "BASIC", "BASIC", "BASIC_AUTO", "false", "0", "0", "0", "0", "180", "NONE"),
        ("SKILL_1", "ULTIMATE", "RAGE_SKILL", "RAGE_FULL", "true", "100", "0", "0", "0", "650", "MINI_CINEMATIC"),
        ("SKILL_2", "SKILL_1", "ACTIVE_SKILL", "COOLDOWN", "false", "0", "6000", "250", "0", "450", "NONE"),
        ("SKILL_3", "SKILL_2", "ACTIVE_SKILL", "COOLDOWN", "false", "0", "9000", "350", "0", "500", "NONE"),
        ("PASSIVE", "PASSIVE", "PASSIVE", "EVENT", "false", "0", "0", "0", "0", "0", "NONE"),
    ]
    out: list[dict[str, str]] = []
    for slot, legacy, kind, trigger, signature, rage_cost, cooldown, cast, impact, recovery, cinematic in plan:
        src = group[legacy]
        skill_id = f"m50-{src['hero_id']}-{slot.lower().replace('_', '-')}"
        out.append({
            "skill_id": skill_id,
            "hero_id": src["hero_id"],
            "slot": slot,
            "legacy_slot": legacy,
            "source_technique_id": src["source_technique_id"],
            "name_en": src["name_en"],
            "name_vi": src["name_vi"],
            "channel": src["channel"],
            "ability_kind": kind,
            "trigger_type": trigger,
            "is_signature": signature,
            "rage_cost": rage_cost,
            "cooldown_ms": cooldown,
            "cast_time_ms": cast,
            "impact_ms": impact,
            "recovery_ms": recovery,
            "cinematic_mode": cinematic,
            "animation_key": f"skills/{src['hero_id']}/{slot.lower()}/animation",
            "vfx_key": f"skills/{src['hero_id']}/{slot.lower()}/vfx",
            "sfx_key": f"skills/{src['hero_id']}/{slot.lower()}/sfx",
            "canon_source": "",
            "canon_confidence": "UNREVIEWED",
            "review_status": "RESEARCH_REQUIRED",
            "research_note": (
                "Structural candidate generated from the M47 explicit alias. M50 must research the exact Hero Version, "
                "confirm/replace this technique, tune realtime timing/effects, write VI/EN runtime-derived descriptions, "
                "and complete cinematic/counterplay/balance evidence before READY."
            ),
        })
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    groups: dict[str, dict[str, dict[str, str]]] = defaultdict(dict)
    with SOURCE.open(encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            hero_id = row["hero_id"].strip()
            slot = row["slot"].strip()
            if slot in groups[hero_id]:
                raise SystemExit(f"duplicate legacy slot {hero_id}/{slot}")
            groups[hero_id][slot] = row

    rows: list[dict[str, str]] = []
    for hero_id in sorted(groups):
        missing = set(LEGACY_REQUIRED) - set(groups[hero_id])
        extra = set(groups[hero_id]) - set(LEGACY_REQUIRED)
        if missing or extra:
            raise SystemExit(f"{hero_id} legacy kit mismatch missing={sorted(missing)} extra={sorted(extra)}")
        rows.extend(migrate(groups[hero_id]))

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDNAMES)
        writer.writeheader()
        writer.writerows(rows)

    print(f"M50_SKILL_DESIGN_CANDIDATE_OK heroes={len(groups)} rows={len(rows)} rage_skills={len(groups)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
