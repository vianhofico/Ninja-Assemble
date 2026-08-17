#!/usr/bin/env python3
"""Validate M50 skill-design candidate against Hero/Awakening production data and review gates."""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HEROES = ROOT / "game-data/heroes/heroes.csv"
AWAKENING_SKILLS = ROOT / "game-data/skills/awakening-skills.csv"
EXPECTED_SLOTS = {"BASIC", "SKILL_1", "SKILL_2", "SKILL_3", "PASSIVE"}
FORBIDDEN_TURN_TRIGGERS = {"TURN_START", "TURN_END", "ROUND_START", "ROUND_END"}
REVIEWED_FIELDS = {
    "name_en", "name_vi", "ability_kind", "trigger_type", "target_selector", "effect_profile_id",
    "description_en", "description_vi", "canon_source", "canon_confidence", "counterplay", "balance_status",
    "animation_key", "vfx_key", "sfx_key",
}


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def require_non_negative_int(row: dict[str, str], field: str) -> int:
    try:
        value = int(row[field])
    except (KeyError, TypeError, ValueError) as exc:
        raise SystemExit(f"{row.get('hero_id')}/{row.get('slot')} invalid integer {field}: {row.get(field)!r}") from exc
    if value < 0:
        raise SystemExit(f"{row['hero_id']}/{row['slot']} negative {field}")
    return value


def validate_ready(row: dict[str, str]) -> None:
    missing = sorted(field for field in REVIEWED_FIELDS if not (row.get(field) or "").strip())
    if missing:
        raise SystemExit(f"{row['hero_id']}/{row['slot']} READY_DESIGN missing fields: {missing}")
    if row["canon_confidence"] == "UNREVIEWED":
        raise SystemExit(f"{row['hero_id']}/{row['slot']} READY_DESIGN cannot be UNREVIEWED")
    if row["balance_status"] not in {"EXPERIMENTAL", "OBSERVED", "VERIFIED"}:
        raise SystemExit(f"{row['hero_id']}/{row['slot']} invalid balance_status={row['balance_status']}")
    for field in ("rage_cost", "rage_gain", "cooldown_ms", "cast_time_ms", "impact_ms", "recovery_ms"):
        require_non_negative_int(row, field)
    if row["trigger_type"] in FORBIDDEN_TURN_TRIGGERS:
        raise SystemExit(f"{row['hero_id']}/{row['slot']} still uses turn/round trigger {row['trigger_type']}")
    if "turn" in row["description_en"].lower() or "round" in row["description_en"].lower() or "lượt" in row["description_vi"].lower():
        raise SystemExit(f"{row['hero_id']}/{row['slot']} description still uses turn/round timing")

    if row["slot"] == "SKILL_1":
        if row["ability_kind"] != "RAGE_SKILL" or row["trigger_type"] != "RAGE_FULL":
            raise SystemExit(f"{row['hero_id']} reviewed Skill 1 is not RAGE_SKILL/RAGE_FULL")
        if row["is_signature"].lower() != "true" or require_non_negative_int(row, "rage_cost") != 100:
            raise SystemExit(f"{row['hero_id']} reviewed Rage Skill signature/cost invalid")
        if row["cinematic_mode"] != "MINI_CINEMATIC":
            raise SystemExit(f"{row['hero_id']} reviewed Rage Skill lacks MINI_CINEMATIC")
    elif row["is_signature"].lower() == "true":
        raise SystemExit(f"{row['hero_id']}/{row['slot']} non-Rage skill cannot be signature=true")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate", required=True)
    args = parser.parse_args()

    hero_rows = read_rows(HEROES)
    hero_ids = {r["hero_id"].strip() for r in hero_rows}
    candidate = read_rows(Path(args.candidate))
    awakening_rows = read_rows(AWAKENING_SKILLS)

    by_hero: dict[str, list[dict[str, str]]] = defaultdict(list)
    skill_ids: set[str] = set()
    for row in candidate:
        hero_id = row["hero_id"].strip()
        skill_id = row["skill_id"].strip()
        if hero_id not in hero_ids:
            raise SystemExit(f"candidate references unknown Hero Version: {hero_id}")
        if not skill_id or skill_id in skill_ids:
            raise SystemExit(f"blank/duplicate M50 skill id: {skill_id}")
        skill_ids.add(skill_id)
        by_hero[hero_id].append(row)
        if row["review_status"] == "READY_DESIGN":
            validate_ready(row)
        elif row["review_status"] != "RESEARCH_REQUIRED":
            raise SystemExit(f"{hero_id}/{row['slot']} unknown review_status={row['review_status']}")

    if set(by_hero) != hero_ids:
        missing = sorted(hero_ids - set(by_hero))
        extra = sorted(set(by_hero) - hero_ids)
        raise SystemExit(f"Hero Version coverage mismatch missing={missing[:10]} extra={extra[:10]}")

    fully_ready_heroes = 0
    for hero_id, rows in by_hero.items():
        if len(rows) != 5:
            raise SystemExit(f"{hero_id} must have exactly five base skills, got {len(rows)}")
        slots = {r["slot"] for r in rows}
        if slots != EXPECTED_SLOTS:
            raise SystemExit(f"{hero_id} invalid final slots: {sorted(slots)}")
        rage = next(r for r in rows if r["slot"] == "SKILL_1")
        if rage["ability_kind"] != "RAGE_SKILL" or rage["trigger_type"] != "RAGE_FULL":
            raise SystemExit(f"{hero_id} SKILL_1 must be RAGE_SKILL/RAGE_FULL")
        if rage["is_signature"].lower() != "true" or int(rage["rage_cost"]) != 100:
            raise SystemExit(f"{hero_id} signature Rage contract invalid")
        if rage["legacy_slot"] != "ULTIMATE":
            raise SystemExit(f"{hero_id} structural candidate must preserve old Ultimate as Rage provenance")
        if rage["cinematic_mode"] != "MINI_CINEMATIC":
            raise SystemExit(f"{hero_id} Rage Skill lacks cinematic contract")
        basic = next(r for r in rows if r["slot"] == "BASIC")
        if basic["ability_kind"] != "BASIC" or basic["trigger_type"] != "BASIC_AUTO":
            raise SystemExit(f"{hero_id} Basic contract invalid")
        passive = next(r for r in rows if r["slot"] == "PASSIVE")
        if passive["ability_kind"] != "PASSIVE" or passive["trigger_type"] in FORBIDDEN_TURN_TRIGGERS:
            raise SystemExit(f"{hero_id} passive contract invalid")
        for active_slot in ("SKILL_2", "SKILL_3"):
            active = next(r for r in rows if r["slot"] == active_slot)
            if active["ability_kind"] != "ACTIVE_SKILL":
                raise SystemExit(f"{hero_id}/{active_slot} active contract invalid")
        if any(r["slot"] == "ULTIMATE" for r in rows):
            raise SystemExit(f"{hero_id} still exposes competing legacy ULTIMATE slot")
        if all(r["review_status"] == "READY_DESIGN" for r in rows):
            fully_ready_heroes += 1

    awakening_hero_ids = [r["hero_id"].strip() for r in awakening_rows]
    if len(awakening_hero_ids) != len(set(awakening_hero_ids)):
        raise SystemExit("duplicate Awakening Skill Hero Version mapping")
    unknown_awakenings = sorted(set(awakening_hero_ids) - hero_ids)
    if unknown_awakenings:
        raise SystemExit(f"Awakening skills reference unknown heroes: {unknown_awakenings[:10]}")

    research_required = sum(1 for row in candidate if row["review_status"] != "READY_DESIGN")
    ready_rows = len(candidate) - research_required
    print(
        f"M50_SKILL_DESIGN_INTEGRITY_OK heroes={len(hero_ids)} base_skills={len(candidate)} "
        f"rage_skills={len(hero_ids)} awakening_skills={len(awakening_rows)} ready_rows={ready_rows} "
        f"fully_ready_heroes={fully_ready_heroes} research_required={research_required}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
