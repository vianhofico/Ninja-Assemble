#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "game-data" / "design"
DOC = ROOT / "docs" / "design" / "HERO_AWAKENING_PAIR_PROPOSAL.md"

# Source-census labels that are abilities/jutsu rather than persistent forms.
# For these characters we introduce a canonical neutral Base form in the research pool,
# keep one collectible Hero Version, and migrate the legacy label to a skill.
SYNTHETIC_BASES: dict[str, dict[str, str]] = {
    "akatsuchi": {
        "legacy": "Stone Golem",
        "hero_name": "Akatsuchi",
        "source": "https://naruto.fandom.com/wiki/Akatsuchi;https://naruto.fandom.com/wiki/Earth_Release%3A_Golem_Technique",
        "reason": "Earth Release: Golem Technique is a jutsu used by Akatsuchi, not a persistent body/form transformation.",
    },
    "darui": {
        "legacy": "Storm Release",
        "hero_name": "Darui",
        "source": "https://naruto.fandom.com/wiki/Darui;https://naruto.fandom.com/wiki/Storm_Release",
        "reason": "Storm Release is Darui's kekkei genkai/nature transformation, not a separate persistent hero form.",
    },
    "dodai": {
        "legacy": "Lava Rubber Style",
        "hero_name": "Dodai",
        "source": "https://naruto.fandom.com/wiki/Dodai;https://naruto.fandom.com/wiki/Lava_Release%3A_Rubber_Rope",
        "reason": "Dodai's vulcanised-rubber Lava Release is expressed through ninjutsu such as Rubber Rope/Defence, not a persistent form.",
    },
    "pakura": {
        "legacy": "Scorch Release",
        "hero_name": "Pakura",
        "source": "https://naruto.fandom.com/wiki/Pakura;https://naruto.fandom.com/wiki/Scorch_Release",
        "reason": "Scorch Release is Pakura's kekkei genkai/nature transformation and should define her kit rather than become her body/form identity.",
    },
    "gari": {
        "legacy": "Explosion Release",
        "hero_name": "Gari",
        "source": "https://naruto.fandom.com/wiki/Gari;https://naruto.fandom.com/wiki/Explosion_Release",
        "reason": "Explosion Release is Gari's kekkei genkai/ninjutsu style, not a persistent transformation.",
    },
    "kurotsuchi": {
        "legacy": "Lava Release",
        "hero_name": "Kurotsuchi",
        "source": "https://naruto.fandom.com/wiki/Kurotsuchi;https://naruto.fandom.com/wiki/Lava_Release",
        "reason": "Lava Release is a nature transformation used through Kurotsuchi's jutsu, not a separate form.",
    },
    "toroi": {
        "legacy": "Magnet Release",
        "hero_name": "Toroi",
        "source": "https://naruto.fandom.com/wiki/Toroi;https://naruto.fandom.com/wiki/Magnet_Release",
        "reason": "Magnet Release is Toroi's kekkei genkai applied to weapons, not a persistent form.",
    },
    "guren": {
        "legacy": "Crystal Release",
        "hero_name": "Guren",
        "source": "https://naruto.fandom.com/wiki/Guren;https://naruto.fandom.com/wiki/Crystal_Release",
        "reason": "Crystal Release is Guren's ninjutsu identity and should feed her five-skill kit rather than become a collectible form label.",
    },
    "yoroi-akado": {
        "legacy": "Chakra Absorption",
        "hero_name": "Yoroi Akado",
        "source": "https://naruto.fandom.com/wiki/Yoroi_Akad%C5%8D",
        "reason": "Chakra absorption is Yoroi's combat ability, not a persistent transformation.",
    },
    "misumi-tsurugi": {
        "legacy": "Soft Physique",
        "hero_name": "Misumi Tsurugi",
        "source": "https://naruto.fandom.com/wiki/Misumi_Tsurugi",
        "reason": "Soft Physique Modification is Misumi's combat technique/body ability, not an Awakening or collectible form.",
    },
}

FORCE_SKILLS: dict[tuple[str, str], str] = {
    ("dan-kato", "Spirit Transformation"): "Spirit Transformation Technique is a jutsu that projects Dan's spirit; it is not a persistent transformed body.",
    ("hiruko", "Chimera Technique"): "Chimera Technique is a technique/power system, not a second persistent Hiruko form label.",
}


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def write(path: Path, fields: list[str], rows: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def synthetic_form_id(character_id: str) -> str:
    return f"form-{character_id}-base"


def skill_id(character_id: str, variant: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", variant.lower()).strip("-")
    return f"legacy-{character_id}-{slug}"


def mark_skill(migration: dict[str, str], form: dict[str, str] | None, reason: str) -> None:
    migration.update({
        "classification": "SKILL_OR_ULTIMATE",
        "new_hero_id": "",
        "new_awakening_form_id": "",
        "new_skill_id": skill_id(migration["character_id"], migration["old_variant"]),
        "new_skin_id": "",
        "temporary_state_id": "",
        "migration_action": "MOVE_TO_SKILL",
        "reason": reason,
        "confidence": "HIGH",
    })
    if form is not None:
        form.update({
            "is_persistent_form": "false",
            "is_temporary_form": "false",
            "is_technique": "true",
            "is_coop": "false",
            "standalone_hero_score": "0",
            "awakening_score": "0",
            "canon_type": "MANGA_CANON",
            "notes": reason,
        })


def patch_doc(doc: str, character_id: str, old_base: str, hero_name: str, reason: str) -> str:
    section_header = f"## {character_id}\n"
    start = doc.find(section_header)
    if start < 0:
        raise ValueError(f"proposal missing character section {character_id}")
    next_start = doc.find("\n## ", start + len(section_header))
    if next_start < 0:
        next_start = len(doc)
    section = doc[start:next_start]
    section = section.replace(f"### {character_id} [Core]", f"### {hero_name}", 1)
    section = section.replace(f"- **Base form:** {old_base}", "- **Base form:** Base", 1)
    section = section.replace("- **Canon confidence:** DESIGN_INTERPRETATION", "- **Canon confidence:** MANGA_CANON", 1)
    section = section.replace("- **Source:** current variant census;", "- **Source:** character canon page + current variant census;", 1)
    bullet = f"- **{old_base}** → `SKILL_OR_ULTIMATE` / `MOVE_TO_SKILL` — {reason}"
    if bullet not in section:
        marker = "### Reclassified / unused source forms\n\n"
        if marker in section:
            section = section.replace(marker, marker + bullet + "\n", 1)
        else:
            section = section.rstrip() + "\n\n### Reclassified / unused source forms\n\n" + bullet + "\n"
    return doc[:start] + section + doc[next_start:]


def main() -> int:
    migration_path = DESIGN / "variant-reclassification.csv"
    forms_path = DESIGN / "character-form-pool.csv"
    pairs_path = DESIGN / "hero-awakening-pairs.csv"
    migration = read(migration_path)
    forms = read(forms_path)
    pairs = read(pairs_path)
    m_idx = {(r["character_id"], r["old_variant"]): r for r in migration}
    f_idx = {(r["character_id"], r["form_name"]): r for r in forms}
    pair_by_char = {}
    for row in pairs:
        if row["hero_id"].endswith("-core"):
            pair_by_char.setdefault(row["character_id"], row)

    doc = DOC.read_text(encoding="utf-8")

    for character_id, cfg in SYNTHETIC_BASES.items():
        legacy = cfg["legacy"]
        key = (character_id, legacy)
        if key not in m_idx or character_id not in pair_by_char:
            raise ValueError(f"synthetic base correction missing generated row/pair: {key}")
        pair = pair_by_char[character_id]
        old_base_form = pair["base_form_id"]
        pair.update({
            "hero_version_name": cfg["hero_name"],
            "base_form_id": synthetic_form_id(character_id),
            "canon_confidence": "MANGA_CANON",
            "source_reference": cfg["source"],
            "design_reason": "Character retained as one collectible base hero; legacy ability label is migrated into the explicit kit instead of being treated as a form.",
            "status": "PROPOSED_RESEARCH_BASELINE",
        })
        mark_skill(m_idx[key], f_idx.get(key), cfg["reason"])
        forms.append({
            "character_id": character_id,
            "form_id": synthetic_form_id(character_id),
            "form_name": "Base",
            "era": "SHIPPUDEN",
            "power_level": "5",
            "visual_difference": "6",
            "combat_difference": "6",
            "is_persistent_form": "true",
            "is_temporary_form": "false",
            "is_technique": "false",
            "is_coop": "false",
            "standalone_hero_score": pair["hero_version_score"],
            "awakening_score": "0",
            "canon_type": "MANGA_CANON",
            "canon_source": cfg["source"],
            "notes": "Synthetic neutral form-pool entry representing the character themselves; not an additional legacy variant.",
        })
        doc = patch_doc(doc, character_id, legacy, cfg["hero_name"], cfg["reason"])

    for key, reason in FORCE_SKILLS.items():
        if key not in m_idx:
            raise ValueError(f"forced skill correction missing source row {key}")
        mark_skill(m_idx[key], f_idx.get(key), reason)
        # Remove an automatically assigned Awakening from the associated core pair.
        expected_awake_form = m_idx[key]["old_variant_id"]
        for pair in pairs:
            if pair["character_id"] == key[0] and pair["awakening_form_id"] == expected_awake_form:
                pair.update({
                    "awakening_form_id": "",
                    "awakening_name": "",
                    "pair_score": "",
                    "awakening_type": "NONE",
                    "design_reason": pair["design_reason"] + " The legacy technique-like label was removed from the Awakening slot after semantic audit.",
                })
                # Patch proposal section.
                section_header = f"## {key[0]}\n"
                s = doc.find(section_header)
                e = doc.find("\n## ", s + len(section_header)) if s >= 0 else -1
                if e < 0:
                    e = len(doc)
                section = doc[s:e]
                section = section.replace(f"- **Awakening:** {key[1]}", "- **Awakening:** None — no persistent canonical transformation reserved", 1)
                section = re.sub(r"- \*\*Pair score:\*\* [^\n]+", "- **Pair score:** N/A", section, count=1)
                bullet = f"- **{key[1]}** → `SKILL_OR_ULTIMATE` / `MOVE_TO_SKILL` — {reason}"
                marker = "### Reclassified / unused source forms\n\n"
                if bullet not in section:
                    if marker in section:
                        section = section.replace(marker, marker + bullet + "\n", 1)
                    else:
                        section = section.rstrip() + "\n\n### Reclassified / unused source forms\n\n" + bullet + "\n"
                doc = doc[:s] + section + doc[e:]
                break

    # Strong semantic invariants for known ability labels.
    forbidden_hero_labels = {cfg["legacy"] for cfg in SYNTHETIC_BASES.values()} | {v for _, v in FORCE_SKILLS}
    errors: list[str] = []
    for pair in pairs:
        base_name = pair["base_form_id"].split("form-", 1)[-1]
        if any(pair["base_form_id"].endswith("-" + re.sub(r"[^a-z0-9]+", "-", label.lower()).strip("-")) for label in forbidden_hero_labels):
            errors.append(f"technique-like label still used as base hero: {pair['hero_id']} {pair['base_form_id']}")
        if any(pair["awakening_form_id"].endswith("-" + re.sub(r"[^a-z0-9]+", "-", label.lower()).strip("-")) for label in FORCE_SKILLS):
            errors.append(f"technique-like label still used as Awakening: {pair['hero_id']} {pair['awakening_form_id']}")
    if errors:
        raise SystemExit("\n".join(errors))

    write(migration_path, list(migration[0]), sorted(migration, key=lambda r: (r["character_id"], r["old_variant"])))
    write(forms_path, list(forms[0]), sorted(forms, key=lambda r: (r["character_id"], r["form_name"])))
    write(pairs_path, list(pairs[0]), sorted(pairs, key=lambda r: (r["character_id"], r["hero_id"])))
    DOC.write_text(doc, encoding="utf-8")
    print(f"HERO_AWAKENING_SEMANTIC_REFINEMENT_OK synthetic_bases={len(SYNTHETIC_BASES)} forced_skills={len(FORCE_SKILLS)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
