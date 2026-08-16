#!/usr/bin/env python3
from __future__ import annotations

import csv
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "game-data" / "design"
SKILLS = ROOT / "game-data" / "skills"
HEROES = ROOT / "game-data" / "heroes" / "heroes.csv"
AWAKENINGS = ROOT / "game-data" / "progression" / "awakenings.csv"
AWAKE_SKILLS = SKILLS / "awakening-skills.csv"
ALIASES = SKILLS / "hero-version-skills.csv"
VISUALS = ROOT / "game-data" / "assets" / "awakening-visuals.csv"
MIGRATION = ROOT / "server" / "src" / "main" / "resources" / "db" / "migration" / "V10__hero_version_awakening_model.sql"
SLOTS = {"BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE"}


def read(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        raise SystemExit(f"missing M42 artifact: {path.relative_to(ROOT)}")
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def techniques() -> set[str]:
    ids: set[str] = set()
    for path in sorted(SKILLS.glob("technique-library-*.csv")):
        for row in read(path):
            tid = row["technique_id"].strip()
            if tid in ids:
                raise SystemExit(f"duplicate source technique {tid}")
            ids.add(tid)
    return ids


def main() -> int:
    errors: list[str] = []
    pairs = read(DESIGN / "hero-awakening-pairs.csv")
    heroes = read(HEROES)
    aliases = read(ALIASES)
    awakenings = read(AWAKENINGS)
    awakening_skills = read(AWAKE_SKILLS)
    visuals = read(VISUALS)
    source_techniques = techniques()

    pair_by_hero = {r["hero_id"].strip(): r for r in pairs}
    hero_by_id = {r["hero_id"].strip(): r for r in heroes}
    awake_by_id = {r["awakening_id"].strip(): r for r in awakenings}
    skill_by_id = {r["awakening_skill_id"].strip(): r for r in awakening_skills}
    visual_by_id = {r["awakening_id"].strip(): r for r in visuals}

    if len(pair_by_hero) != len(pairs):
        errors.append("proposal has duplicate hero_id")
    if set(hero_by_id) != set(pair_by_hero):
        errors.append(f"heroes.csv must exactly match proposal hero IDs proposal={len(pair_by_hero)} heroes={len(hero_by_id)}")
    if len(heroes) != 194:
        errors.append(f"M42 baseline expected 194 Hero Versions from M41, got {len(heroes)}")

    aliases_by_hero: dict[str, list[dict[str, str]]] = defaultdict(list)
    alias_ids: set[str] = set()
    for row in aliases:
        sid = row["skill_id"].strip()
        if sid in alias_ids:
            errors.append(f"duplicate hero-version skill alias {sid}")
        alias_ids.add(sid)
        aliases_by_hero[row["hero_id"].strip()].append(row)
        source = row["source_technique_id"].strip()
        if source not in source_techniques:
            errors.append(f"alias {sid} references missing source technique {source}")
        if row["explicitness"].strip() != "HERO_VERSION_EXPLICIT_ALIAS":
            errors.append(f"alias {sid} is not explicit per Hero Version")

    if len(aliases) != len(heroes) * 5:
        errors.append(f"exactly five base slots required per hero: aliases={len(aliases)} heroes={len(heroes)}")
    for hero_id, hero in hero_by_id.items():
        rows = aliases_by_hero.get(hero_id, [])
        slots = [r["slot"].strip() for r in rows]
        if len(rows) != 5 or set(slots) != SLOTS or len(slots) != len(set(slots)):
            errors.append(f"{hero_id}: expected exactly one alias in each of five slots, got {slots}")
        expected = {
            "BASIC": hero["basic_skill"].strip(), "SKILL_1": hero["skill_1"].strip(),
            "SKILL_2": hero["skill_2"].strip(), "ULTIMATE": hero["ultimate"].strip(),
            "PASSIVE": hero["passive"].strip(),
        }
        actual = {r["slot"].strip(): r["skill_id"].strip() for r in rows}
        if expected != actual:
            errors.append(f"{hero_id}: heroes.csv five-slot references do not match alias rows")
        if hero["summonable"].strip().lower() != "true":
            errors.append(f"{hero_id}: collectible Hero Version must be summonable in this catalog")
        pair = pair_by_hero[hero_id]
        expected_awake = f"awakening-{hero_id}" if pair["awakening_form_id"].strip() else ""
        if hero["awakening_id"].strip() != expected_awake:
            errors.append(f"{hero_id}: awakening_id does not match M41 proposal")

    proposed_awake = [r for r in pairs if r["awakening_form_id"].strip()]
    if len(awakenings) != len(proposed_awake):
        errors.append(f"Awakening count must match proposal pairs expected={len(proposed_awake)} actual={len(awakenings)}")
    if len(awakenings) != 61:
        errors.append(f"M42 baseline expected 61 Awakenings, got {len(awakenings)}")
    if len(awake_by_id) != len(awakenings):
        errors.append("duplicate awakening_id")
    awakened_forms = [r["awakened_form_id"].strip() for r in awakenings]
    if len(awakened_forms) != len(set(awakened_forms)):
        errors.append("an Awakening form is reused by multiple Hero Versions")
    base_forms = {r["base_form_id"].strip() for r in heroes}
    collision = base_forms & set(awakened_forms)
    if collision:
        errors.append(f"persistent form cannot be both BASE HERO and AWAKENING: {sorted(collision)[:5]}")

    hero_awake_count = Counter(r["hero_id"].strip() for r in awakenings)
    for hero_id, count in hero_awake_count.items():
        if count > 1:
            errors.append(f"{hero_id}: has more than one Awakening")
    for row in awakenings:
        aid = row["awakening_id"].strip()
        hero_id = row["hero_id"].strip()
        if hero_id not in hero_by_id:
            errors.append(f"{aid}: unknown hero {hero_id}")
            continue
        pair = pair_by_hero[hero_id]
        if row["base_form_id"].strip() != pair["base_form_id"].strip() or row["awakened_form_id"].strip() != pair["awakening_form_id"].strip():
            errors.append(f"{aid}: base/awakened forms drift from M41 proposal")
        skill_id = row["awakening_skill_id"].strip()
        if skill_id not in skill_by_id:
            errors.append(f"{aid}: missing exactly-one Awakening Skill {skill_id}")
        if aid not in visual_by_id:
            errors.append(f"{aid}: missing Awakening visual specification")
        for field in ("base_model", "awakened_model", "base_portrait", "awakened_portrait", "awakening_animation", "awakening_vfx", "awakening_sfx"):
            if not row[field].strip():
                errors.append(f"{aid}: blank visual runtime key {field}")
        if row["base_model"].strip() == row["awakened_model"].strip() or row["base_portrait"].strip() == row["awakened_portrait"].strip():
            errors.append(f"{aid}: base and awakened presentation keys must differ visibly")

    if len(skill_by_id) != len(awakening_skills) or len(awakening_skills) != len(awakenings):
        errors.append("every Awakening requires exactly one unique Awakening Skill row")
    if len(visual_by_id) != len(visuals) or len(visuals) != len(awakenings):
        errors.append("every Awakening requires exactly one unique visual spec row")
    for row in awakening_skills:
        if row["classification"].strip() != "AWAKENING_SKILL":
            errors.append(f"{row['awakening_skill_id']}: invalid classification")
        if row["status"].strip() != "SCHEMA_BASELINE_NOT_RUNTIME":
            errors.append(f"{row['awakening_skill_id']}: M42 must not pretend generic Awakening mechanics are runtime/final")
        if row["special_mechanic"].strip() != "M47_EXPLICIT_DESIGN_REQUIRED":
            errors.append(f"{row['awakening_skill_id']}: final explicit skill design must remain gated to M47")

    sql_text = MIGRATION.read_text(encoding="utf-8") if MIGRATION.exists() else ""
    required_sql = [
        "create table hero_versions", "create table hero_version_skill_aliases", "create table awakening_definitions",
        "create table awakening_skill_definitions", "create table awakening_visual_definitions",
        "alter table player_heroes add column hero_version_id", "add column awakened boolean not null default false",
        "M43 will backfill player_heroes.hero_version_id",
    ]
    for token in required_sql:
        if token not in sql_text:
            errors.append(f"V10 migration missing required token: {token}")
    if sql_text.count("insert into hero_versions ") != len(heroes):
        errors.append("V10 hero seed count does not match heroes.csv")
    if sql_text.count("insert into hero_version_skill_aliases ") != len(aliases):
        errors.append("V10 five-slot alias seed count does not match hero-version-skills.csv")
    if sql_text.count("insert into awakening_definitions ") != len(awakenings):
        errors.append("V10 Awakening seed count does not match awakenings.csv")

    if errors:
        print("AWAKENING_PRODUCTION_DATA_INVALID")
        for error in errors:
            print(" -", error)
        return 1

    print(
        f"AWAKENING_PRODUCTION_DATA_VALID heroes={len(heroes)} base_skills={len(aliases)} "
        f"awakenings={len(awakenings)} awakening_skills={len(awakening_skills)} visuals={len(visuals)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
