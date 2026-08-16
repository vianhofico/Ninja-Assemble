#!/usr/bin/env python3
"""Final Hero/Awakening migration gate for M48."""
from __future__ import annotations

import csv
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECLASS = ROOT / "game-data/design/variant-reclassification.csv"
HEROES = ROOT / "game-data/heroes/heroes.csv"
AWAKENINGS = ROOT / "game-data/progression/awakenings.csv"
AWAKENING_SKILLS = ROOT / "game-data/skills/awakening-skills.csv"
ALIASES = ROOT / "game-data/skills/hero-version-skills.csv"
BRIDGE = ROOT / "game-data/migration/legacy-hero-version-map.csv"

ALLOWED_CLASSIFICATIONS = {
    "COLLECTIBLE_HERO_VERSION",
    "AWAKENING_FORM",
    "SKILL_OR_ULTIMATE",
    "TEMPORARY_COMBAT_FORM",
    "COSMETIC_SKIN",
    "COOPERATION_FORM_OR_TECHNIQUE",
    "SPECIAL_INDEPENDENT_CHARACTER",
    "MERGED_OR_REMOVED_DUPLICATE",
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def reject_runtime(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    bad = [token for token in tokens if token in text]
    if bad:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID runtime legacy path={path} tokens={bad}")


def main() -> int:
    classified = rows(RECLASS)
    heroes = rows(HEROES)
    awakenings = rows(AWAKENINGS)
    awakening_skills = rows(AWAKENING_SKILLS)
    aliases = rows(ALIASES)
    bridge = rows(BRIDGE)

    if len(classified) != 427:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID classified={len(classified)} expected=427")
    source_keys = [(r["character_id"], r["old_variant"]) for r in classified]
    if len(set(source_keys)) != len(source_keys):
        raise SystemExit("M48_FINAL_CUTOVER_INVALID duplicate legacy variant rows")
    unknown = sorted({r["classification"] for r in classified} - ALLOWED_CLASSIFICATIONS)
    if unknown:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID unknown classifications={unknown}")

    hero_ids = [r["hero_id"] for r in heroes]
    if len(hero_ids) != 194 or len(set(hero_ids)) != 194:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID heroes={len(hero_ids)} expected=194 unique={len(set(hero_ids))}")
    if any((r.get("summonable") or "").lower() != "true" for r in heroes):
        raise SystemExit("M48_FINAL_CUTOVER_INVALID every heroes.csv row must be an independently collectible Hero Version")

    awakening_ids = [r["awakening_id"] for r in awakenings]
    if len(awakening_ids) != 60 or len(set(awakening_ids)) != 60:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID awakenings={len(awakening_ids)} expected=60 unique={len(set(awakening_ids))}")
    awakening_hero_ids = [r["hero_id"] for r in awakenings]
    if len(set(awakening_hero_ids)) != len(awakening_hero_ids):
        raise SystemExit("M48_FINAL_CUTOVER_INVALID Hero Version has more than one Awakening")
    if not set(awakening_hero_ids).issubset(set(hero_ids)):
        raise SystemExit("M48_FINAL_CUTOVER_INVALID Awakening references unknown Hero Version")

    collectible_forms = {r["old_variant_id"] for r in classified if r["classification"] == "COLLECTIBLE_HERO_VERSION"}
    awakening_forms = {r["old_variant_id"] for r in classified if r["classification"] == "AWAKENING_FORM"}
    overlap = sorted(collectible_forms & awakening_forms)
    if overlap:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID form is both hero and Awakening={overlap[:10]}")

    if len(aliases) != 194 * 5:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID base skill aliases={len(aliases)} expected=970")
    alias_counts = Counter(r["hero_id"] for r in aliases)
    if any(alias_counts.get(hero_id) != 5 for hero_id in hero_ids):
        raise SystemExit("M48_FINAL_CUTOVER_INVALID every Hero Version must have exactly five base aliases")
    if len(awakening_skills) != 60 or {r["hero_id"] for r in awakening_skills} != set(awakening_hero_ids):
        raise SystemExit("M48_FINAL_CUTOVER_INVALID Awakening Skill coverage must be exactly one per Awakening")

    # Every original source row must still have an auditable bridge row by its original label.
    bridge_keys = {(r["legacy_character_id"], r["legacy_variant_id"]) for r in bridge}
    missing_bridge = [(c, v) for c, v in source_keys if (c, v) not in bridge_keys]
    if missing_bridge:
        raise SystemExit(f"M48_FINAL_CUTOVER_INVALID bridge missing={missing_bridge[:10]}")

    # Legacy compatibility is allowed in migration/ownership internals, never as playable endpoints/UI.
    reject_runtime("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
                   "/variant", "VariantRequest", "selectVariant(")
    reject_runtime("server/src/main/java/com/ninjaassemble/progression/api/HeroProgressionController.java",
                   "/evolve", "/evolution-paths", "targetVariant")
    reject_runtime("client-unity/Assets/Scripts/Game/Heroes/HeroProgressionPlayableBridge.cs",
                   "EVOLVE", "/evolve", "targetVariant", "EvolutionPath")

    classifications = Counter(r["classification"] for r in classified)
    summary = ",".join(f"{k}={classifications[k]}" for k in sorted(classifications))
    print(
        f"M48_FINAL_CUTOVER_OK variants=427 heroes={len(heroes)} awakenings={len(awakenings)} "
        f"base_skills={len(aliases)} awakening_skills={len(awakening_skills)} bridge_rows={len(bridge)} classifications={summary}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
