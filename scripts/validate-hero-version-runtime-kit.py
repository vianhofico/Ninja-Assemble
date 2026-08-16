#!/usr/bin/env python3
from __future__ import annotations

import csv
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKILLS = ROOT / "game-data" / "skills"
HEROES = ROOT / "game-data" / "heroes" / "heroes.csv"


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def main() -> int:
    errors: list[str] = []
    heroes = read(HEROES)
    aliases = read(SKILLS / "hero-version-skills.csv")
    awakenings = read(ROOT / "game-data" / "progression" / "awakenings.csv")
    awakening_skills = read(SKILLS / "awakening-skills.csv")

    hero_ids = {r["hero_id"].strip() for r in heroes}
    if len(hero_ids) != len(heroes) or len(heroes) != 194:
        errors.append(f"expected 194 unique runtime Hero Versions, got rows={len(heroes)} unique={len(hero_ids)}")

    techniques: set[str] = set()
    for path in sorted(SKILLS.glob("technique-library-*.csv")):
        for row in read(path):
            tid = row["technique_id"].strip()
            if tid in techniques:
                errors.append(f"duplicate source technique {tid}")
            techniques.add(tid)

    by_hero: defaultdict[str, list[dict[str, str]]] = defaultdict(list)
    alias_ids: set[str] = set()
    expected_slots = {"BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE"}
    for row in aliases:
        sid = row["skill_id"].strip()
        hero_id = row["hero_id"].strip()
        if sid in alias_ids: errors.append(f"duplicate explicit skill id {sid}")
        alias_ids.add(sid)
        if hero_id not in hero_ids: errors.append(f"skill alias {sid} references unknown hero {hero_id}")
        if row["source_technique_id"].strip() not in techniques:
            errors.append(f"skill alias {sid} references missing source technique {row['source_technique_id']}")
        by_hero[hero_id].append(row)
    if len(aliases) != len(heroes) * 5:
        errors.append(f"runtime requires exactly five aliases per hero: aliases={len(aliases)} heroes={len(heroes)}")
    for hero_id in sorted(hero_ids):
        rows = by_hero.get(hero_id, [])
        slots = [r["slot"].strip() for r in rows]
        if len(rows) != 5 or set(slots) != expected_slots or len(slots) != len(set(slots)):
            errors.append(f"{hero_id}: invalid five-slot base kit {slots}")

    awake_by_hero = Counter(r["hero_id"].strip() for r in awakenings)
    skill_by_hero = Counter(r["hero_id"].strip() for r in awakening_skills)
    if len(awakenings) != len(awakening_skills):
        errors.append("Awakening and sixth-skill counts differ")
    for hero in heroes:
        hero_id = hero["hero_id"].strip()
        has_awake = bool(hero["awakening_id"].strip())
        if awake_by_hero[hero_id] != (1 if has_awake else 0):
            errors.append(f"{hero_id}: expected {'one' if has_awake else 'zero'} Awakening definition")
        if skill_by_hero[hero_id] != (1 if has_awake else 0):
            errors.append(f"{hero_id}: expected {'one' if has_awake else 'zero'} Awakening Skill")

    for row in awakening_skills:
        if row["status"].strip() != "SCHEMA_BASELINE_NOT_RUNTIME":
            errors.append(f"{row['awakening_skill_id']}: M44 must not execute an unresolved sixth skill before M47")

    content_path = ROOT / "server/src/main/java/com/ninjaassemble/hero/catalog/HeroContentCatalogService.java"
    content = content_path.read_text(encoding="utf-8")
    for forbidden in ("character-kit-map.csv", "variant-kit-overrides.csv", "kit-profiles.csv"):
        if forbidden in content:
            errors.append(f"runtime content catalog still reads forbidden fallback source {forbidden}")
    for required in ("hero-version-skills.csv", "heroes/heroes.csv", "awakening-skills.csv", "legacy-hero-version-map.csv", "resolveHero"):
        if required not in content:
            errors.append(f"runtime content catalog missing explicit source/API {required}")

    pure_resolver = (ROOT / "server/src/main/java/com/ninjaassemble/hero/kit/HeroKitResolver.java").read_text(encoding="utf-8")
    for forbidden in ("characterProfiles", "variantOverrides", "VariantKey"):
        if forbidden in pure_resolver:
            errors.append(f"HeroKitResolver still contains legacy fallback symbol {forbidden}")
    if "resolve(String heroId, boolean awakened)" not in pure_resolver:
        errors.append("HeroKitResolver must resolve by heroId + awakened")

    player_runtime_paths = [
        ROOT / "server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java",
        ROOT / "server/src/main/java/com/ninjaassemble/pvp/application/ArenaApplicationService.java",
        ROOT / "server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java",
    ]
    for path in player_runtime_paths:
        text = path.read_text(encoding="utf-8")
        if "stats.resolve" not in text:
            errors.append(f"expected battle resolver use missing in {path.name}")
        if "stats.resolve(prefix + hero.id(), hero.characterId(), hero.currentVariant()" in text:
            errors.append(f"{path.name} still resolves player combat by character+variant")
        if "stats.resolve(\n                    hero.id().toString(), hero.characterId(), hero.currentVariant()" in text:
            errors.append(f"{path.name} still resolves player combat by character+variant")

    stats = (ROOT / "server/src/main/java/com/ninjaassemble/play/domain/ExperimentalCombatStatsResolver.java").read_text(encoding="utf-8")
    if "resolve(String battleUnitId, String heroId, boolean awakened" not in stats:
        errors.append("combat stats resolver has no Hero Version production overload")
    if "content.resolveHero(heroId, awakened)" not in stats:
        errors.append("combat stats resolver does not use explicit Hero Version kit")

    if errors:
        print("HERO_VERSION_RUNTIME_KIT_INVALID")
        for error in errors:
            print(" -", error)
        return 1
    print(f"HERO_VERSION_RUNTIME_KIT_VALID heroes={len(heroes)} base_slots={len(aliases)} awakenings={len(awakenings)} sixth_skills={len(awakening_skills)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
