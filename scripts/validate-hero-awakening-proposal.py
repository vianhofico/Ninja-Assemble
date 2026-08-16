#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REF = ROOT / "game-data" / "reference"
DESIGN = ROOT / "game-data" / "design"
DOC = ROOT / "docs" / "design" / "HERO_AWAKENING_PAIR_PROPOSAL.md"

VALID_CLASSES = {
    "COLLECTIBLE_HERO_VERSION", "AWAKENING_FORM", "SKILL_OR_ULTIMATE",
    "TEMPORARY_COMBAT_FORM", "COSMETIC_SKIN", "COOPERATION_FORM_OR_TECHNIQUE",
    "SPECIAL_INDEPENDENT_CHARACTER", "MERGED_OR_REMOVED_DUPLICATE",
}
KNOWN_SKILL_ONLY = {
    ("akatsuchi", "Stone Golem"),
    ("darui", "Storm Release"),
    ("dodai", "Lava Rubber Style"),
    ("pakura", "Scorch Release"),
    ("gari", "Explosion Release"),
    ("kurotsuchi", "Lava Release"),
    ("toroi", "Magnet Release"),
    ("guren", "Crystal Release"),
    ("yoroi-akado", "Chakra Absorption"),
    ("misumi-tsurugi", "Soft Physique"),
    ("dan-kato", "Spirit Transformation"),
    ("hiruko", "Chimera Technique"),
    ("deidara", "C2 Dragon"),
    ("deidara", "C4"),
    ("sasori", "Hundred Puppets"),
    ("chiyo", "Ten Puppets"),
    ("hidan", "Curse Ritual"),
}
SYNTHETIC_BASES = {
    "akatsuchi", "darui", "dodai", "pakura", "gari", "kurotsuchi", "toroi", "guren", "yoroi-akado", "misumi-tsurugi"
}


def read(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        raise SystemExit(f"missing required proposal artifact: {path.relative_to(ROOT)}")
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def source_variants() -> set[tuple[str, str]]:
    out: set[tuple[str, str]] = set()
    for path in sorted(REF.glob("variant-census*.csv")):
        for row in read(path):
            out.add((row["character_id"].strip(), row["variant"].strip()))
    return out


def main() -> int:
    errors: list[str] = []
    source = source_variants()
    migrations = read(DESIGN / "variant-reclassification.csv")
    forms = read(DESIGN / "character-form-pool.csv")
    pairs = read(DESIGN / "hero-awakening-pairs.csv")

    migration_keys = [(r["character_id"].strip(), r["old_variant"].strip()) for r in migrations]
    if len(source) != 427:
        errors.append(f"current source census must remain 427 for this migration, got {len(source)}")
    if len(migration_keys) != len(set(migration_keys)):
        errors.append("variant-reclassification contains duplicate source keys")
    if set(migration_keys) != source:
        missing = source - set(migration_keys)
        extra = set(migration_keys) - source
        errors.append(f"variant coverage mismatch missing={len(missing)} extra={len(extra)}")
    for row in migrations:
        if row["classification"].strip() not in VALID_CLASSES:
            errors.append(f"invalid classification {row['classification']!r} for {row['character_id']}::{row['old_variant']}")

    form_ids = [r["form_id"].strip() for r in forms]
    if len(form_ids) != len(set(form_ids)):
        errors.append("character-form-pool has duplicate form_id")
    form_set = set(form_ids)

    hero_ids = [r["hero_id"].strip() for r in pairs]
    if not hero_ids or len(hero_ids) != len(set(hero_ids)):
        errors.append("hero_id must be non-empty and unique")
    awakening_ids = [r["awakening_form_id"].strip() for r in pairs if r["awakening_form_id"].strip()]
    if len(awakening_ids) != len(set(awakening_ids)):
        errors.append("each Awakening form may belong to exactly one hero")
    base_ids = {r["base_form_id"].strip() for r in pairs}
    awake_ids = set(awakening_ids)
    collision = base_ids & awake_ids
    if collision:
        errors.append(f"forms cannot be both base hero and Awakening: {sorted(collision)[:5]}")

    for row in pairs:
        hero_id = row["hero_id"].strip()
        base = row["base_form_id"].strip()
        awake = row["awakening_form_id"].strip()
        if base not in form_set:
            errors.append(f"{hero_id}: missing base form {base}")
        if awake and awake not in form_set:
            errors.append(f"{hero_id}: missing Awakening form {awake}")
        try:
            hero_score = int(row["hero_version_score"])
        except ValueError:
            errors.append(f"{hero_id}: invalid hero score")
            hero_score = 0
        if hero_score < 65:
            errors.append(f"{hero_id}: hero score below 65")
        if awake:
            try:
                pair_score = int(row["pair_score"])
            except ValueError:
                errors.append(f"{hero_id}: Awakening requires numeric pair score")
                pair_score = 0
            if pair_score < 70:
                errors.append(f"{hero_id}: pair score below 70")

    mig = {(r["character_id"].strip(), r["old_variant"].strip()): r for r in migrations}
    for key in sorted(KNOWN_SKILL_ONLY):
        row = mig.get(key)
        if row is None:
            errors.append(f"known semantic row missing: {key}")
            continue
        if row["classification"].strip() != "SKILL_OR_ULTIMATE":
            errors.append(f"known technique still misclassified: {key} -> {row['classification']}")

    forms_by_key = {(r["character_id"].strip(), r["form_name"].strip()): r for r in forms}
    for character_id in sorted(SYNTHETIC_BASES):
        synthetic = forms_by_key.get((character_id, "Base"))
        if synthetic is None or synthetic.get("is_persistent_form", "").lower() != "true":
            errors.append(f"{character_id}: missing persistent synthetic Base form")
        char_pairs = [r for r in pairs if r["character_id"].strip() == character_id]
        if len(char_pairs) != 1 or char_pairs[0]["base_form_id"].strip() != f"form-{character_id}-base":
            errors.append(f"{character_id}: collectible hero must resolve to synthetic Base form")

    # Major non-overlap expectations from the researched proposal.
    expected = {
        "naruto-genin": "form-naruto-uzumaki-one-tail-cloak",
        "naruto-sage": "form-naruto-uzumaki-kcm2",
        "naruto-six-paths": "form-naruto-uzumaki-asura-kurama-mode",
        "naruto-hokage": "form-naruto-uzumaki-baryon-mode",
        "sasuke-genin": "form-sasuke-uchiha-curse-mark-level-2",
        "sasuke-hebi": "form-sasuke-uchiha-mangekyo-sharingan",
        "sasuke-ems": "form-sasuke-uchiha-rinnegan",
        "sakura-shippuden": "form-sakura-haruno-byakugo",
        "kakashi-war": "form-kakashi-hatake-double-mangekyo",
        "itachi-akatsuki": "form-itachi-uchiha-susanoo",
        "madara-rinnegan": "form-madara-uchiha-ten-tails-jinchuriki",
        "obito-white-mask": "form-obito-uchiha-ten-tails-jinchuriki",
        "might-guy-base": "form-might-guy-eighth-gate",
    }
    by_hero = {r["hero_id"].strip(): r for r in pairs}
    for hero_id, awake in expected.items():
        row = by_hero.get(hero_id)
        if row is None or row["awakening_form_id"].strip() != awake:
            errors.append(f"major pair regression: {hero_id} expected Awakening {awake}")

    doc = DOC.read_text(encoding="utf-8") if DOC.exists() else ""
    for token in ("Source variants classified: **427 / 427**", "No form is both collectible Hero Version and Awakening"):
        if token not in doc:
            errors.append(f"proposal document missing invariant text: {token}")

    if errors:
        print("HERO_AWAKENING_PROPOSAL_INVALID")
        for error in errors:
            print(" -", error)
        return 1

    print(
        "HERO_AWAKENING_PROPOSAL_VALID "
        f"variants={len(source)} heroes={len(pairs)} awakenings={len(awakening_ids)} forms={len(forms)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
