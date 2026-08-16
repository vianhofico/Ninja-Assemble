#!/usr/bin/env python3
"""M47 full-catalog Hero Version skill identity/canon-risk validator.

M47 does not invent final Rage/timing values. It locks the structural identity of
all five base slots plus the single Awakening slot, detects duplicated same-character
kits, and makes research debt visible for the mandatory M50 design pass.
"""
from __future__ import annotations

import csv
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
HEROES = ROOT / "game-data/heroes/heroes.csv"
ALIASES = ROOT / "game-data/skills/hero-version-skills.csv"
AWAKENING_SKILLS = ROOT / "game-data/skills/awakening-skills.csv"
TECHNIQUE_FILES = [ROOT / f"game-data/skills/technique-library-0{i}.csv" for i in range(1, 5)]

SLOTS = ("BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE")
BASELINE_MARKERS = (
    "PLAYABLE_DESIGN_BASELINE",
    "M47 must tune",
    "M47_EXPLICIT_DESIGN_REQUIRED",
    "SCHEMA_BASELINE_NOT_RUNTIME",
    "UNRESOLVED_EXPLICIT_DESIGN",
)


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def require(value: str | None, message: str) -> str:
    if value is None or not value.strip():
        raise SystemExit(f"M47_SKILL_IDENTITY_INVALID {message}")
    return value.strip()


def main() -> int:
    heroes = rows(HEROES)
    aliases = rows(ALIASES)
    awakening_skills = rows(AWAKENING_SKILLS)

    if not heroes:
        raise SystemExit("M47_SKILL_IDENTITY_INVALID no Hero Versions")

    hero_by_id: dict[str, dict[str, str]] = {}
    for hero in heroes:
        hero_id = require(hero.get("hero_id"), "hero_id required")
        if hero_id in hero_by_id:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID duplicate hero_id={hero_id}")
        hero_by_id[hero_id] = hero

    techniques: set[str] = set()
    for path in TECHNIQUE_FILES:
        for row in rows(path):
            technique_id = require(row.get("technique_id"), f"technique_id required in {path.name}")
            if technique_id in techniques:
                raise SystemExit(f"M47_SKILL_IDENTITY_INVALID duplicate technique_id={technique_id}")
            techniques.add(technique_id)

    alias_by_hero: dict[str, dict[str, dict[str, str]]] = defaultdict(dict)
    seen_skill_ids: set[str] = set()
    baseline_base_slots = 0
    for alias in aliases:
        skill_id = require(alias.get("skill_id"), "base skill_id required")
        hero_id = require(alias.get("hero_id"), f"{skill_id} hero_id required")
        slot = require(alias.get("slot"), f"{skill_id} slot required")
        source = require(alias.get("source_technique_id"), f"{skill_id} source technique required")
        if skill_id in seen_skill_ids:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID duplicate skill_id={skill_id}")
        seen_skill_ids.add(skill_id)
        if hero_id not in hero_by_id:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID {skill_id} unknown hero={hero_id}")
        if slot not in SLOTS:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID {skill_id} invalid slot={slot}")
        if slot in alias_by_hero[hero_id]:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID {hero_id} duplicate slot={slot}")
        if source not in techniques:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID {skill_id} missing source technique={source}")
        alias_by_hero[hero_id][slot] = alias
        combined = " | ".join(alias.get(key, "") for key in ("status", "research_note", "explicitness"))
        if any(marker in combined for marker in BASELINE_MARKERS):
            baseline_base_slots += 1

    if len(aliases) != len(heroes) * len(SLOTS):
        raise SystemExit(
            f"M47_SKILL_IDENTITY_INVALID expected {len(heroes) * len(SLOTS)} base slots got {len(aliases)}"
        )

    for hero_id, hero in hero_by_id.items():
        actual = set(alias_by_hero[hero_id])
        if actual != set(SLOTS):
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID {hero_id} slots={sorted(actual)}")
        for slot, hero_column in (
            ("BASIC", "basic_skill"),
            ("SKILL_1", "skill_1"),
            ("SKILL_2", "skill_2"),
            ("ULTIMATE", "ultimate"),
            ("PASSIVE", "passive"),
        ):
            expected_skill = require(hero.get(hero_column), f"{hero_id} {hero_column} required")
            actual_skill = alias_by_hero[hero_id][slot]["skill_id"]
            if expected_skill != actual_skill:
                raise SystemExit(
                    f"M47_SKILL_IDENTITY_INVALID {hero_id} {slot} hero_csv={expected_skill} alias={actual_skill}"
                )

    # Versions of the same character must not secretly share an identical five-technique identity.
    character_kits: dict[str, dict[tuple[str, ...], list[str]]] = defaultdict(lambda: defaultdict(list))
    for hero_id, hero in hero_by_id.items():
        character_id = require(hero.get("character_id"), f"{hero_id} character_id required")
        signature = tuple(alias_by_hero[hero_id][slot]["source_technique_id"] for slot in SLOTS)
        character_kits[character_id][signature].append(hero_id)
    duplicate_same_character_kits: list[tuple[str, list[str]]] = []
    for character_id, signatures in character_kits.items():
        for hero_ids in signatures.values():
            if len(hero_ids) > 1:
                duplicate_same_character_kits.append((character_id, sorted(hero_ids)))
    if duplicate_same_character_kits:
        detail = "; ".join(f"{character}:{'|'.join(ids)}" for character, ids in duplicate_same_character_kits[:20])
        raise SystemExit(f"M47_SKILL_IDENTITY_INVALID identical same-character five-slot kits: {detail}")

    # `heroes.csv` is the production source of truth for one-Awakening ownership.
    # Design proposal CSVs intentionally use form IDs, while runtime references a stable awakening_id.
    awakening_by_hero: dict[str, str] = {}
    awakening_ids: set[str] = set()
    for hero_id, hero in hero_by_id.items():
        awakening_id = (hero.get("awakening_id") or "").strip()
        if not awakening_id:
            continue
        if awakening_id in awakening_ids:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID duplicate awakening_id={awakening_id}")
        awakening_ids.add(awakening_id)
        awakening_by_hero[hero_id] = awakening_id

    awakening_skill_by_hero: dict[str, dict[str, str]] = {}
    baseline_awakening_slots = 0
    for skill in awakening_skills:
        skill_id = require(skill.get("awakening_skill_id"), "awakening_skill_id required")
        hero_id = require(skill.get("hero_id"), f"{skill_id} hero_id required")
        awakening_id = require(skill.get("awakening_id"), f"{skill_id} awakening_id required")
        if hero_id in awakening_skill_by_hero:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID >1 Awakening Skill hero={hero_id}")
        if hero_id not in awakening_by_hero:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID Awakening Skill without Awakening hero={hero_id}")
        if awakening_by_hero[hero_id] != awakening_id:
            raise SystemExit(f"M47_SKILL_IDENTITY_INVALID Awakening Skill pair mismatch hero={hero_id}")
        require(skill.get("name_en"), f"{skill_id} name_en required")
        require(skill.get("name_vi"), f"{skill_id} name_vi required")
        require(skill.get("canon_source"), f"{skill_id} canon_source required")
        require(skill.get("canon_confidence"), f"{skill_id} canon_confidence required")
        combined = " | ".join(skill.get(key, "") for key in ("status", "effects", "special_mechanic", "description_en"))
        if any(marker in combined for marker in BASELINE_MARKERS):
            baseline_awakening_slots += 1
        awakening_skill_by_hero[hero_id] = skill

    if set(awakening_skill_by_hero) != set(awakening_by_hero):
        missing = sorted(set(awakening_by_hero) - set(awakening_skill_by_hero))
        extra = sorted(set(awakening_skill_by_hero) - set(awakening_by_hero))
        raise SystemExit(f"M47_SKILL_IDENTITY_INVALID Awakening Skill coverage missing={missing[:10]} extra={extra[:10]}")

    era_counts = Counter(hero.get("era", "") or "UNSPECIFIED" for hero in heroes)
    multi_version_characters = sum(1 for signatures in character_kits.values() if sum(len(v) for v in signatures.values()) > 1)
    print(
        "M47_SKILL_IDENTITY_OK "
        f"heroes={len(heroes)} base_slots={len(aliases)} awakenings={len(awakening_by_hero)} "
        f"awakening_slots={len(awakening_skills)} techniques={len(techniques)} "
        f"multi_version_characters={multi_version_characters} "
        f"baseline_base_slots={baseline_base_slots} baseline_awakening_slots={baseline_awakening_slots} "
        f"eras={len(era_counts)}"
    )
    print(
        "M47_RESEARCH_DEBT "
        "baseline counts are intentionally visible and must be driven to zero by #54/M50 before final skill-design completion"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
