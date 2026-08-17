#!/usr/bin/env python3
"""Generate the authoritative M50 five-slot skill-design catalog.

The explicit M47 Hero Version aliases remain migration provenance. M50 converts them into
BASIC / SKILL_1(Rage) / SKILL_2 / SKILL_3 / PASSIVE and overlays reviewed per-Hero-Version
batch files named m50-skill-overrides*.csv. A structural candidate is never silently
promoted to READY: only an explicit override may advance review_status.
"""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "game-data/skills/hero-version-skills.csv"
OVERRIDE_DIR = ROOT / "game-data/skills"
TECHNIQUE_RESOURCES = [ROOT / f"game-data/skills/technique-library-0{i}.csv" for i in range(1, 5)]
FINAL_SLOTS = ("BASIC", "SKILL_1", "SKILL_2", "SKILL_3", "PASSIVE")
LEGACY_REQUIRED = ("BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE")

FIELDNAMES = [
    "skill_id", "hero_id", "slot", "legacy_slot", "source_technique_id",
    "name_en", "name_vi", "channel", "ability_kind", "trigger_type", "is_signature",
    "rage_cost", "rage_gain", "cooldown_ms", "cast_time_ms", "impact_ms", "recovery_ms",
    "target_selector", "effect_profile_id", "special_mechanic",
    "cinematic_mode", "animation_key", "vfx_key", "sfx_key", "voice_line",
    "description_en", "description_vi", "canon_source", "canon_confidence", "counterplay",
    "balance_status", "review_status", "research_note",
]

OVERRIDE_KEY_FIELDS = ("hero_id", "slot")
OVERRIDABLE_FIELDS = tuple(field for field in FIELDNAMES if field not in {"skill_id", "hero_id", "slot", "legacy_slot"})


def read_techniques() -> dict[str, dict[str, str]]:
    result: dict[str, dict[str, str]] = {}
    for path in TECHNIQUE_RESOURCES:
        with path.open(encoding="utf-8", newline="") as handle:
            for row in csv.DictReader(handle):
                technique_id = row["technique_id"].strip()
                if technique_id in result:
                    raise SystemExit(f"duplicate technique id across libraries: {technique_id}")
                result[technique_id] = row
    return result


def read_overrides() -> dict[tuple[str, str], dict[str, str]]:
    result: dict[tuple[str, str], dict[str, str]] = {}
    paths = sorted(OVERRIDE_DIR.glob("m50-skill-overrides*.csv"))
    for path in paths:
        with path.open(encoding="utf-8", newline="") as handle:
            reader = csv.DictReader(handle)
            required = set(OVERRIDE_KEY_FIELDS) | {"review_status"}
            if not reader.fieldnames or not required.issubset(reader.fieldnames):
                raise SystemExit(f"{path.name} missing fields: {sorted(required - set(reader.fieldnames or []))}")
            unknown = set(reader.fieldnames) - (set(OVERRIDE_KEY_FIELDS) | set(OVERRIDABLE_FIELDS))
            if unknown:
                raise SystemExit(f"{path.name} has unknown fields: {sorted(unknown)}")
            for row in reader:
                if not any((value or "").strip() for value in row.values()):
                    continue
                key = (row["hero_id"].strip(), row["slot"].strip())
                if key in result:
                    raise SystemExit(f"duplicate M50 override across batches: {key[0]}/{key[1]}")
                if key[1] not in FINAL_SLOTS:
                    raise SystemExit(f"unknown M50 override slot: {key[0]}/{key[1]}")
                result[key] = row
    return result


def technique_defaults(techniques: dict[str, dict[str, str]], technique_id: str) -> dict[str, str]:
    technique = techniques.get(technique_id)
    if technique is None:
        raise SystemExit(f"M50 references unknown source technique: {technique_id}")
    return {
        "name_en": technique.get("name_en", "").strip(),
        "name_vi": technique.get("name_vi", "").strip(),
        "channel": technique.get("channel", "MIXED").strip() or "MIXED",
    }


def migrate(group: dict[str, dict[str, str]], techniques: dict[str, dict[str, str]]) -> list[dict[str, str]]:
    plan = [
        ("BASIC", "BASIC", "BASIC", "BASIC_AUTO", "false", "0", "15", "0", "0", "0", "180", "NONE"),
        ("SKILL_1", "ULTIMATE", "RAGE_SKILL", "RAGE_FULL", "true", "100", "0", "0", "0", "0", "650", "MINI_CINEMATIC"),
        ("SKILL_2", "SKILL_1", "ACTIVE_SKILL", "COOLDOWN", "false", "0", "0", "6000", "250", "0", "450", "NONE"),
        ("SKILL_3", "SKILL_2", "ACTIVE_SKILL", "COOLDOWN", "false", "0", "0", "9000", "350", "0", "500", "NONE"),
        ("PASSIVE", "PASSIVE", "PASSIVE", "EVENT", "false", "0", "0", "0", "0", "0", "0", "NONE"),
    ]
    out: list[dict[str, str]] = []
    for slot, legacy, kind, trigger, signature, rage_cost, rage_gain, cooldown, cast, impact, recovery, cinematic in plan:
        src = group[legacy]
        skill_id = f"m50-{src['hero_id']}-{slot.lower().replace('_', '-')}"
        defaults = technique_defaults(techniques, src["source_technique_id"])
        out.append({
            "skill_id": skill_id,
            "hero_id": src["hero_id"], "slot": slot, "legacy_slot": legacy,
            "source_technique_id": src["source_technique_id"],
            "name_en": defaults["name_en"] or src["name_en"],
            "name_vi": defaults["name_vi"] or src["name_vi"],
            "channel": defaults["channel"] or src["channel"],
            "ability_kind": kind, "trigger_type": trigger, "is_signature": signature,
            "rage_cost": rage_cost, "rage_gain": rage_gain, "cooldown_ms": cooldown,
            "cast_time_ms": cast, "impact_ms": impact, "recovery_ms": recovery,
            "target_selector": "FRONTMOST_ENEMY" if slot != "PASSIVE" else "SELF",
            "effect_profile_id": src["source_technique_id"], "special_mechanic": "",
            "cinematic_mode": cinematic,
            "animation_key": f"skills/{src['hero_id']}/{slot.lower()}/animation",
            "vfx_key": f"skills/{src['hero_id']}/{slot.lower()}/vfx",
            "sfx_key": f"skills/{src['hero_id']}/{slot.lower()}/sfx",
            "voice_line": "", "description_en": "", "description_vi": "",
            "canon_source": "", "canon_confidence": "UNREVIEWED", "counterplay": "",
            "balance_status": "EXPERIMENTAL", "review_status": "RESEARCH_REQUIRED",
            "research_note": (
                "Structural candidate generated from the explicit M47 alias. M50 must research the exact Hero Version, "
                "confirm/replace the technique, define effects/targets/realtime timing/counterplay, write EN/VI descriptions, "
                "and complete cinematic design for Rage Skills before READY_DESIGN."
            ),
        })
    return out


def apply_overrides(rows: list[dict[str, str]], overrides: dict[tuple[str, str], dict[str, str]],
                    techniques: dict[str, dict[str, str]]) -> None:
    seen: set[tuple[str, str]] = set()
    for row in rows:
        key = (row["hero_id"], row["slot"])
        override = overrides.get(key)
        if override is None:
            continue
        seen.add(key)
        for field in OVERRIDABLE_FIELDS:
            value = (override.get(field) or "").strip()
            if value:
                row[field] = value
        if (override.get("source_technique_id") or "").strip():
            defaults = technique_defaults(techniques, row["source_technique_id"])
            for field in ("name_en", "name_vi", "channel"):
                if not (override.get(field) or "").strip():
                    row[field] = defaults[field]
        if not row["effect_profile_id"]:
            row["effect_profile_id"] = row["source_technique_id"]
    orphaned = sorted(set(overrides) - seen)
    if orphaned:
        raise SystemExit(f"M50 overrides reference unknown Hero Version/slot: {orphaned[:10]}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    techniques = read_techniques()
    overrides = read_overrides()
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
        rows.extend(migrate(groups[hero_id], techniques))

    apply_overrides(rows, overrides, techniques)

    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=FIELDNAMES)
        writer.writeheader(); writer.writerows(rows)

    ready = sum(1 for row in rows if row["review_status"] == "READY_DESIGN")
    print(
        f"M50_SKILL_DESIGN_CANDIDATE_OK heroes={len(groups)} rows={len(rows)} "
        f"rage_skills={len(groups)} override_batches={len(list(OVERRIDE_DIR.glob('m50-skill-overrides*.csv')))} "
        f"overrides={len(overrides)} ready_design={ready}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
