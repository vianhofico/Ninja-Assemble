#!/usr/bin/env python3
"""Validate M50 structural skill-design candidate against Hero/Awakening production data."""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HEROES = ROOT / "game-data/heroes/heroes.csv"
AWAKENING_SKILLS = ROOT / "game-data/skills/awakening-skills.csv"
EXPECTED_SLOTS = {"BASIC", "SKILL_1", "SKILL_2", "SKILL_3", "PASSIVE"}


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


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

    if set(by_hero) != hero_ids:
        missing = sorted(hero_ids - set(by_hero))
        extra = sorted(set(by_hero) - hero_ids)
        raise SystemExit(f"Hero Version coverage mismatch missing={missing[:10]} extra={extra[:10]}")

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
        if passive["ability_kind"] != "PASSIVE" or passive["trigger_type"] != "EVENT":
            raise SystemExit(f"{hero_id} passive contract invalid")
        for active_slot in ("SKILL_2", "SKILL_3"):
            active = next(r for r in rows if r["slot"] == active_slot)
            if active["ability_kind"] != "ACTIVE_SKILL" or active["trigger_type"] != "COOLDOWN":
                raise SystemExit(f"{hero_id}/{active_slot} active contract invalid")
        if any(r["slot"] == "ULTIMATE" for r in rows):
            raise SystemExit(f"{hero_id} still exposes competing legacy ULTIMATE slot")

    awakening_hero_ids = [r["hero_id"].strip() for r in awakening_rows]
    if len(awakening_hero_ids) != len(set(awakening_hero_ids)):
        raise SystemExit("duplicate Awakening Skill Hero Version mapping")
    unknown_awakenings = sorted(set(awakening_hero_ids) - hero_ids)
    if unknown_awakenings:
        raise SystemExit(f"Awakening skills reference unknown heroes: {unknown_awakenings[:10]}")

    research_required = sum(1 for row in candidate if row["review_status"] != "READY")
    print(
        f"M50_SKILL_DESIGN_INTEGRITY_OK heroes={len(hero_ids)} base_skills={len(candidate)} "
        f"rage_skills={len(hero_ids)} awakening_skills={len(awakening_rows)} research_required={research_required}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
