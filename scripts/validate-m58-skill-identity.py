#!/usr/bin/env python3
"""M58 full-roster skill identity/canon/editorial audit.

Normal mode validates structure and reports unresolved editorial debt. --enforce is a
production gate: every Hero Version and Awakening Skill must have reviewed identity
metadata and no exact duplicate reviewed full kit may remain.
"""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
GENERATOR = ROOT / "scripts/generate-m50-skill-design.py"
M50_VALIDATOR = ROOT / "scripts/validate-m50-skill-design.py"
AWAKENINGS = ROOT / "game-data/skills/awakening-skills.csv"
EXPECTED_HEROES = 194
EXPECTED_BASE_SKILLS = EXPECTED_HEROES * 5
EXPECTED_AWAKENINGS = 60
SLOTS = ("BASIC", "SKILL_1", "SKILL_2", "SKILL_3", "PASSIVE")
FINAL_AWAKENING_STATUSES = {"IDENTITY_REVIEWED", "READY_DESIGN", "RUNTIME_READY", "READY"}
DEBT_TOKENS = {"RESEARCH_REQUIRED", "UNREVIEWED", "UNRESOLVED_EXPLICIT_DESIGN", "M47_EXPLICIT_DESIGN_REQUIRED"}
BASE_IDENTITY_FIELDS = {
    "source_technique_id", "name_en", "name_vi", "ability_kind", "trigger_type",
    "description_en", "description_vi", "canon_source", "canon_confidence", "counterplay",
}
AWAKENING_IDENTITY_FIELDS = {
    "awakening_skill_id", "hero_id", "awakening_id", "name_en", "name_vi",
    "description_en", "description_vi", "canon_source", "canon_confidence", "status",
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def contains_debt(value: str) -> str | None:
    upper = (value or "").strip().upper()
    for token in DEBT_TOKENS:
        if token in upper:
            return token
    return None


def require_fields(row: dict[str, str], fields: set[str], label: str) -> None:
    missing = sorted(field for field in fields if not (row.get(field) or "").strip())
    if missing:
        raise ValueError(f"{label}: missing identity fields {missing}")


def reviewed_base(row: dict[str, str]) -> bool:
    return (row.get("review_status") or "").strip().upper() == "READY_DESIGN"


def reviewed_awakening(row: dict[str, str]) -> bool:
    return (row.get("status") or "").strip().upper() in FINAL_AWAKENING_STATUSES


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--enforce", action="store_true")
    args = parser.parse_args()

    try:
        with tempfile.TemporaryDirectory(prefix="m58-skill-identity-") as tmp:
            candidate_path = Path(tmp) / "m50-skill-candidate.csv"
            subprocess.run(
                [sys.executable, str(GENERATOR), "--output", str(candidate_path)],
                cwd=ROOT, check=True,
            )
            subprocess.run(
                [sys.executable, str(M50_VALIDATOR), "--candidate", str(candidate_path)],
                cwd=ROOT, check=True,
            )
            candidate = rows(candidate_path)

        awakenings = rows(AWAKENINGS)
        by_hero: dict[str, list[dict[str, str]]] = defaultdict(list)
        for row in candidate:
            by_hero[(row.get("hero_id") or "").strip()].append(row)

        if len(by_hero) != EXPECTED_HEROES:
            raise ValueError(f"expected {EXPECTED_HEROES} Hero Versions, found {len(by_hero)}")
        if len(candidate) != EXPECTED_BASE_SKILLS:
            raise ValueError(f"expected {EXPECTED_BASE_SKILLS} base skill rows, found {len(candidate)}")
        if len(awakenings) != EXPECTED_AWAKENINGS:
            raise ValueError(f"expected {EXPECTED_AWAKENINGS} Awakening Skills, found {len(awakenings)}")

        ready_rows = 0
        fully_reviewed = 0
        unresolved_rows: list[str] = []
        reviewed_kits: dict[tuple[str, ...], list[str]] = defaultdict(list)

        for hero_id, kit in sorted(by_hero.items()):
            if not hero_id:
                raise ValueError("blank hero_id in M50 candidate")
            slot_map = {row.get("slot", "").strip(): row for row in kit}
            if set(slot_map) != set(SLOTS) or len(kit) != 5:
                raise ValueError(f"{hero_id}: invalid five-slot identity structure")

            hero_ready = True
            source_tuple: list[str] = []
            for slot in SLOTS:
                row = slot_map[slot]
                source_tuple.append((row.get("source_technique_id") or "").strip())
                if reviewed_base(row):
                    require_fields(row, BASE_IDENTITY_FIELDS, f"{hero_id}/{slot}")
                    if contains_debt(row.get("canon_confidence", "")):
                        raise ValueError(f"{hero_id}/{slot}: reviewed row has unresolved canon confidence")
                    if contains_debt(row.get("research_note", "")):
                        raise ValueError(f"{hero_id}/{slot}: reviewed row retains research debt")
                    ready_rows += 1
                else:
                    hero_ready = False
                    unresolved_rows.append(f"{hero_id}/{slot}")

            if hero_ready:
                fully_reviewed += 1
                reviewed_kits[tuple(source_tuple)].append(hero_id)

        duplicate_reviewed_kits = {
            kit: heroes for kit, heroes in reviewed_kits.items() if len(heroes) > 1
        }

        awakening_ready = 0
        unresolved_awakenings: list[str] = []
        awakening_heroes: set[str] = set()
        for row in awakenings:
            hero_id = (row.get("hero_id") or "").strip()
            if not hero_id or hero_id not in by_hero:
                raise ValueError(f"Awakening references unknown Hero Version: {hero_id!r}")
            if hero_id in awakening_heroes:
                raise ValueError(f"duplicate Awakening Skill for Hero Version: {hero_id}")
            awakening_heroes.add(hero_id)
            if reviewed_awakening(row):
                require_fields(row, AWAKENING_IDENTITY_FIELDS, f"awakening/{hero_id}")
                for field in ("name_en", "name_vi", "description_en", "description_vi", "canon_source", "canon_confidence"):
                    token = contains_debt(row.get(field, ""))
                    if token:
                        raise ValueError(f"awakening/{hero_id}: reviewed identity contains {token} in {field}")
                awakening_ready += 1
            else:
                unresolved_awakenings.append(hero_id)

        print(
            "M58_SKILL_IDENTITY_AUDIT "
            f"heroes={len(by_hero)} base_skills={len(candidate)} ready_base={ready_rows} "
            f"fully_reviewed_heroes={fully_reviewed} unresolved_base={len(unresolved_rows)} "
            f"awakenings={len(awakenings)} ready_awakenings={awakening_ready} "
            f"unresolved_awakenings={len(unresolved_awakenings)} duplicate_reviewed_kits={len(duplicate_reviewed_kits)}"
        )
        if unresolved_rows:
            print("M58_RESEARCH_REQUIRED_BASE " + ",".join(unresolved_rows[:25]))
        if unresolved_awakenings:
            print("M58_RESEARCH_REQUIRED_AWAKENING " + ",".join(sorted(unresolved_awakenings)[:25]))
        if duplicate_reviewed_kits:
            preview = ["/".join(heroes) for heroes in list(duplicate_reviewed_kits.values())[:10]]
            print("M58_DUPLICATE_REVIEWED_KITS " + ",".join(preview))

        if args.enforce:
            blockers = []
            if ready_rows != EXPECTED_BASE_SKILLS:
                blockers.append(f"base identity review {ready_rows}/{EXPECTED_BASE_SKILLS}")
            if fully_reviewed != EXPECTED_HEROES:
                blockers.append(f"fully reviewed Hero Versions {fully_reviewed}/{EXPECTED_HEROES}")
            if awakening_ready != EXPECTED_AWAKENINGS:
                blockers.append(f"Awakening identity review {awakening_ready}/{EXPECTED_AWAKENINGS}")
            if duplicate_reviewed_kits:
                blockers.append(f"duplicate reviewed full kits={len(duplicate_reviewed_kits)}")
            if blockers:
                print("M58_SKILL_IDENTITY_BLOCKED " + "; ".join(blockers), file=sys.stderr)
                return 1

        print("M58_SKILL_IDENTITY_OK structure=1 truthful_review_state=1")
        return 0
    except (ValueError, subprocess.CalledProcessError) as error:
        print(f"M58_SKILL_IDENTITY_INVALID {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
