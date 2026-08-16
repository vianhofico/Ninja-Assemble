#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "game-data" / "design"
SKILLS = ROOT / "game-data" / "skills"
REF = ROOT / "game-data" / "reference"
HEROES_DIR = ROOT / "game-data" / "heroes"
PROG = ROOT / "game-data" / "progression"
ASSETS = ROOT / "game-data" / "assets"
MIGRATION = ROOT / "server" / "src" / "main" / "resources" / "db" / "migration" / "V10__hero_version_awakening_model.sql"

SLOTS = ["BASIC", "SKILL_1", "SKILL_2", "ULTIMATE", "PASSIVE"]
PROFILE_COLUMNS = {
    "BASIC": "basic",
    "SKILL_1": "skill1",
    "SKILL_2": "skill2",
    "ULTIMATE": "ultimate",
    "PASSIVE": "passive",
}


def read(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def write(path: Path, fields: list[str], rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fields})


def slug(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")


def sql(value: object) -> str:
    if value is None or value == "":
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def load_techniques() -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    for path in sorted(SKILLS.glob("technique-library-*.csv")):
        for row in read(path):
            tid = row["technique_id"].strip()
            if tid in out:
                raise ValueError(f"duplicate technique_id {tid}")
            out[tid] = row
    return out


def faction(group: str) -> str:
    value = slug(group).upper().replace("-", "_")
    return value or "UNALIGNED"


def hero_element(profile_id: str, techniques: list[dict[str, str]]) -> str:
    channels = {t["channel"].strip().upper() for t in techniques if t.get("channel", "").strip()}
    if channels == {"PHYSICAL"}:
        return "PHYSICAL"
    if channels == {"CHAKRA"}:
        return "CHAKRA"
    if profile_id in {"fire", "water", "wind", "earth", "lightning"}:
        return profile_id.upper()
    return "MIXED"


def rarity(score: int) -> str:
    if score >= 95:
        return "UR"
    if score >= 85:
        return "SSR"
    return "SR"


def profile_for(character_id: str, base_form_id: str, forms_by_id: dict[str, dict[str, str]],
                char_profiles: dict[str, str], overrides: dict[tuple[str, str], str]) -> str:
    form = forms_by_id.get(base_form_id)
    if form is None:
        raise ValueError(f"missing base form {base_form_id}")
    variant = form["form_name"].strip()
    return overrides.get((character_id, variant)) or char_profiles.get(character_id, "")


def awakening_visual_row(pair: dict[str, str], awakening: dict[str, object]) -> dict[str, object]:
    hero_id = pair["hero_id"].strip()
    awakened_name = pair["awakening_name"].strip()
    base = f"client-unity/Assets/GameContent/Heroes/{hero_id}/Base"
    awake = f"client-unity/Assets/GameContent/Heroes/{hero_id}/Awakened"
    return {
        "awakening_id": awakening["awakening_id"],
        "hero_id": hero_id,
        "body_change": f"Persistent silhouette changes from base hero to {awakened_name}.",
        "face_change": f"Use canon-appropriate face markings/features for {awakened_name}; NONE where canon shows no change.",
        "eye_change": f"Use canon-appropriate eye/dojutsu state for {awakened_name}; NONE where not applicable.",
        "hair_change": "Preserve canonical hair unless the awakened form visibly changes it.",
        "clothing_change": f"Use canonical clothing/form treatment for {awakened_name}.",
        "armor_change": f"Use canonical armor/avatar layer for {awakened_name}; NONE when not applicable.",
        "weapon_change": f"Use canonical weapon state for {awakened_name}; NONE when not applicable.",
        "aura_change": f"Distinct awakened aura profile: aura/{hero_id}/awakened",
        "chakra_change": f"Distinct chakra/material profile: chakra/{hero_id}/awakened",
        "summon_change": f"Form-specific summon/avatar layer for {awakened_name}; NONE when not canonically applicable.",
        "transition_start": f"{awake}/Transition/Start",
        "transition_mid": f"{awake}/Transition/Mid",
        "transition_end": f"{awake}/Transition/End",
        "idle_animation": f"{awake}/Animations/Idle",
        "movement_animation": f"{awake}/Animations/Move",
        "basic_vfx_modifier": f"vfx/{hero_id}/awakened/basic",
        "skill1_vfx_modifier": f"vfx/{hero_id}/awakened/skill1",
        "skill2_vfx_modifier": f"vfx/{hero_id}/awakened/skill2",
        "ultimate_vfx_modifier": f"vfx/{hero_id}/awakened/ultimate",
        "awakening_skill_vfx": f"vfx/{hero_id}/awakened/awakening-skill",
        "camera_sequence": f"camera/{hero_id}/awakening",
        "screen_effect": f"screenfx/{hero_id}/awakening",
        "sfx_description": f"Unique transition and form aura SFX for {awakened_name}.",
        "reference_source": pair["source_reference"].strip(),
        "status": "ASSET_SPEC_PENDING_PRODUCTION",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    pairs = read(DESIGN / "hero-awakening-pairs.csv")
    forms = read(DESIGN / "character-form-pool.csv")
    forms_by_id = {r["form_id"].strip(): r for r in forms}
    techniques = load_techniques()
    kit_profiles = {r["profile_id"].strip(): r for r in read(SKILLS / "kit-profiles.csv")}
    char_profiles = {r["character_id"].strip(): r["profile_id"].strip() for r in read(SKILLS / "character-kit-map.csv")}
    overrides = {(r["character_id"].strip(), r["variant"].strip()): r["profile_id"].strip()
                 for r in read(SKILLS / "variant-kit-overrides.csv")}
    roster = {r["id"].strip(): r for r in read(REF / "roster-complete.csv")}

    hero_rows: list[dict[str, object]] = []
    alias_rows: list[dict[str, object]] = []
    awakening_rows: list[dict[str, object]] = []
    awakening_skill_rows: list[dict[str, object]] = []
    visual_rows: list[dict[str, object]] = []
    errors: list[str] = []

    for pair in pairs:
        hero_id = pair["hero_id"].strip()
        character_id = pair["character_id"].strip()
        profile_id = profile_for(character_id, pair["base_form_id"].strip(), forms_by_id, char_profiles, overrides)
        if not profile_id:
            errors.append(f"{hero_id}: no explicit source kit profile for character/base form")
            continue
        profile = kit_profiles.get(profile_id)
        if profile is None:
            errors.append(f"{hero_id}: unknown kit profile {profile_id}")
            continue

        source_skills: list[dict[str, str]] = []
        alias_by_slot: dict[str, str] = {}
        for slot in SLOTS:
            source_id = profile[PROFILE_COLUMNS[slot]].strip()
            source = techniques.get(source_id)
            if source is None:
                errors.append(f"{hero_id}: source technique missing for {slot}: {source_id}")
                continue
            source_skills.append(source)
            alias_id = f"hvs-{hero_id}-{slot.lower().replace('_', '-')}"
            alias_by_slot[slot] = alias_id
            alias_rows.append({
                "skill_id": alias_id,
                "hero_id": hero_id,
                "slot": slot,
                "source_technique_id": source_id,
                "name_en": source["name_en"].strip(),
                "name_vi": source["name_vi"].strip(),
                "channel": source["channel"].strip(),
                "kind": source["kind"].strip(),
                "tags": source["tags"].strip(),
                "explicitness": "HERO_VERSION_EXPLICIT_ALIAS",
                "status": "PLAYABLE_DESIGN_BASELINE",
                "research_note": f"Explicit per-version alias seeded from {profile_id}/{source_id}; M47 must tune version-specific mechanics and evidence before final canon parity.",
            })
        if len(alias_by_slot) != 5:
            continue

        score = int(pair["hero_version_score"])
        group = roster.get(character_id, {}).get("group", "Unaligned")
        display = pair["hero_version_name"].strip()
        awakening_id = f"awakening-{hero_id}" if pair["awakening_form_id"].strip() else ""
        hero_rows.append({
            "hero_id": hero_id,
            "character_id": character_id,
            "version_name": display,
            "display_name_en": display,
            "display_name_vi": display,
            "era": pair["era"].strip(),
            "rarity": rarity(score),
            "role": pair["role"].strip(),
            "element": hero_element(profile_id, source_skills),
            "faction": faction(group),
            "base_form_id": pair["base_form_id"].strip(),
            "basic_skill": alias_by_slot["BASIC"],
            "skill_1": alias_by_slot["SKILL_1"],
            "skill_2": alias_by_slot["SKILL_2"],
            "ultimate": alias_by_slot["ULTIMATE"],
            "passive": alias_by_slot["PASSIVE"],
            "awakening_id": awakening_id,
            "summonable": "true",
            "status": "PLAYABLE_DESIGN_BASELINE",
        })

        if awakening_id:
            awakened_form = pair["awakening_form_id"].strip()
            awakened_name = pair["awakening_name"].strip()
            skill_id = f"awaken-skill-{hero_id}"
            base_model = f"hero/{hero_id}/base/prefab"
            awakened_model = f"hero/{hero_id}/awakened/prefab"
            base_portrait = f"hero/{hero_id}/base/portrait"
            awakened_portrait = f"hero/{hero_id}/awakened/portrait"
            awakening = {
                "awakening_id": awakening_id,
                "hero_id": hero_id,
                "base_form_id": pair["base_form_id"].strip(),
                "awakened_form_id": awakened_form,
                "name_en": awakened_name,
                "name_vi": awakened_name,
                "unlock_level": 60,
                "unlock_rank": "KAGE",
                "awakening_skill_id": skill_id,
                "stat_modifier_profile": "awakening-balanced-v1",
                "base_model": base_model,
                "awakened_model": awakened_model,
                "base_portrait": base_portrait,
                "awakened_portrait": awakened_portrait,
                "awakening_animation": f"animation/{hero_id}/awakening-transition",
                "awakening_vfx": f"vfx/{hero_id}/awakening-transition",
                "awakening_sfx": f"sfx/{hero_id}/awakening-transition",
                "canon_source": pair["source_reference"].strip(),
                "canon_confidence": pair["canon_confidence"].strip(),
                "status": "PLAYABLE_SCHEMA_BASELINE",
            }
            awakening_rows.append(awakening)
            awakening_skill_rows.append({
                "awakening_skill_id": skill_id,
                "hero_id": hero_id,
                "awakening_id": awakening_id,
                "name_en": f"{awakened_name} Signature",
                "name_vi": f"Tuyệt kỹ {awakened_name}",
                "classification": "AWAKENING_SKILL",
                "element": hero_rows[-1]["element"],
                "damage_type": "UNRESOLVED_EXPLICIT_DESIGN",
                "target_type": "UNRESOLVED_EXPLICIT_DESIGN",
                "coefficient": 0,
                "chakra_cost": 0,
                "cooldown": 0,
                "effects": "M47_EXPLICIT_DESIGN_REQUIRED",
                "statuses": "",
                "buffs": "",
                "debuffs": "",
                "special_mechanic": "M47_EXPLICIT_DESIGN_REQUIRED",
                "animation_key": f"animation/{hero_id}/awakening-skill",
                "vfx_key": f"vfx/{hero_id}/awakening-skill",
                "sfx_key": f"sfx/{hero_id}/awakening-skill",
                "description_en": f"Reserved sixth skill for {awakened_name}; final targeting, scaling, counterplay and canon technique selection are intentionally deferred to M47 explicit skill research.",
                "description_vi": f"Kỹ năng thứ sáu dành riêng cho {awakened_name}; mục tiêu, hệ số, khắc chế và kỹ thuật canon cuối cùng sẽ được nghiên cứu/thiết kế rõ ở M47.",
                "canon_source": pair["source_reference"].strip(),
                "canon_confidence": pair["canon_confidence"].strip(),
                "status": "SCHEMA_BASELINE_NOT_RUNTIME",
            })
            visual_rows.append(awakening_visual_row(pair, awakening))

    if errors:
        for error in errors:
            print("ERROR", error)
        return 1

    # Hard M42 invariants.
    if len(hero_rows) != len(pairs):
        raise SystemExit(f"hero row count mismatch pairs={len(pairs)} heroes={len(hero_rows)}")
    hero_ids = [str(r["hero_id"]) for r in hero_rows]
    if len(hero_ids) != len(set(hero_ids)):
        raise SystemExit("duplicate hero_id")
    if len(alias_rows) != len(hero_rows) * 5:
        raise SystemExit(f"every hero needs exactly 5 explicit aliases, got aliases={len(alias_rows)} heroes={len(hero_rows)}")
    awakening_ids = [str(r["awakening_id"]) for r in awakening_rows]
    if len(awakening_ids) != len(set(awakening_ids)):
        raise SystemExit("duplicate awakening_id")
    awakened_forms = [str(r["awakened_form_id"]) for r in awakening_rows]
    if len(awakened_forms) != len(set(awakened_forms)):
        raise SystemExit("awakening form reused")
    if len(awakening_rows) != len(awakening_skill_rows) or len(awakening_rows) != len(visual_rows):
        raise SystemExit("every Awakening requires exactly one Awakening Skill and one visual specification")
    base_forms = {str(r["base_form_id"]) for r in hero_rows}
    if base_forms & set(awakened_forms):
        raise SystemExit("form collision: a persistent form is both base hero and Awakening")

    if args.write:
        write(HEROES_DIR / "heroes.csv", [
            "hero_id", "character_id", "version_name", "display_name_en", "display_name_vi", "era", "rarity",
            "role", "element", "faction", "base_form_id", "basic_skill", "skill_1", "skill_2", "ultimate",
            "passive", "awakening_id", "summonable", "status"
        ], hero_rows)
        write(SKILLS / "hero-version-skills.csv", [
            "skill_id", "hero_id", "slot", "source_technique_id", "name_en", "name_vi", "channel", "kind",
            "tags", "explicitness", "status", "research_note"
        ], alias_rows)
        write(PROG / "awakenings.csv", [
            "awakening_id", "hero_id", "base_form_id", "awakened_form_id", "name_en", "name_vi", "unlock_level",
            "unlock_rank", "awakening_skill_id", "stat_modifier_profile", "base_model", "awakened_model",
            "base_portrait", "awakened_portrait", "awakening_animation", "awakening_vfx", "awakening_sfx",
            "canon_source", "canon_confidence", "status"
        ], awakening_rows)
        write(SKILLS / "awakening-skills.csv", [
            "awakening_skill_id", "hero_id", "awakening_id", "name_en", "name_vi", "classification", "element",
            "damage_type", "target_type", "coefficient", "chakra_cost", "cooldown", "effects", "statuses", "buffs",
            "debuffs", "special_mechanic", "animation_key", "vfx_key", "sfx_key", "description_en", "description_vi",
            "canon_source", "canon_confidence", "status"
        ], awakening_skill_rows)
        write(ASSETS / "awakening-visuals.csv", [
            "awakening_id", "hero_id", "body_change", "face_change", "eye_change", "hair_change", "clothing_change",
            "armor_change", "weapon_change", "aura_change", "chakra_change", "summon_change", "transition_start",
            "transition_mid", "transition_end", "idle_animation", "movement_animation", "basic_vfx_modifier",
            "skill1_vfx_modifier", "skill2_vfx_modifier", "ultimate_vfx_modifier", "awakening_skill_vfx",
            "camera_sequence", "screen_effect", "sfx_description", "reference_source", "status"
        ], visual_rows)
        MIGRATION.parent.mkdir(parents=True, exist_ok=True)
        MIGRATION.write_text(render_migration(hero_rows, alias_rows, awakening_rows, awakening_skill_rows, visual_rows), encoding="utf-8")

    print(
        f"AWAKENING_PRODUCTION_DATA_OK heroes={len(hero_rows)} base_skills={len(alias_rows)} "
        f"awakenings={len(awakening_rows)} awakening_skills={len(awakening_skill_rows)} visuals={len(visual_rows)}"
    )
    return 0


def render_migration(heroes, aliases, awakenings, awakening_skills, visuals) -> str:
    lines = [
        "-- Generated by scripts/generate-awakening-production-data.py. Do not hand-edit.",
        "-- M42 introduces the one-Awakening schema alongside legacy variant data; runtime cutover happens in M43/M44.",
        "",
        "create table hero_versions (",
        "    hero_id varchar(128) primary key,",
        "    character_id varchar(128) not null,",
        "    version_name varchar(160) not null,",
        "    display_name_en varchar(160) not null, display_name_vi varchar(160) not null,",
        "    era varchar(64) not null, rarity varchar(16) not null, role varchar(64) not null,",
        "    element varchar(32) not null, faction varchar(96) not null, base_form_id varchar(192) not null unique,",
        "    basic_skill varchar(192) not null, skill_1 varchar(192) not null, skill_2 varchar(192) not null,",
        "    ultimate varchar(192) not null, passive varchar(192) not null,",
        "    awakening_id varchar(192), summonable boolean not null default true, status varchar(48) not null,",
        "    created_at timestamptz not null default now()",
        ");",
        "create table hero_version_skill_aliases (",
        "    skill_id varchar(192) primary key, hero_id varchar(128) not null references hero_versions(hero_id) on delete cascade,",
        "    slot varchar(16) not null check (slot in ('BASIC','SKILL_1','SKILL_2','ULTIMATE','PASSIVE')),",
        "    source_technique_id varchar(192) not null, name_en varchar(192) not null, name_vi varchar(192) not null,",
        "    channel varchar(32) not null, kind varchar(32) not null, tags text not null default '',",
        "    explicitness varchar(48) not null, status varchar(48) not null, research_note text not null default '',",
        "    unique(hero_id, slot)",
        ");",
        "create table awakening_definitions (",
        "    awakening_id varchar(192) primary key, hero_id varchar(128) not null unique references hero_versions(hero_id) on delete cascade,",
        "    base_form_id varchar(192) not null, awakened_form_id varchar(192) not null unique,",
        "    name_en varchar(192) not null, name_vi varchar(192) not null, unlock_level integer not null check (unlock_level > 0),",
        "    unlock_rank varchar(32) not null, awakening_skill_id varchar(192) not null unique, stat_modifier_profile varchar(96) not null,",
        "    base_model varchar(256) not null, awakened_model varchar(256) not null, base_portrait varchar(256) not null, awakened_portrait varchar(256) not null,",
        "    awakening_animation varchar(256) not null, awakening_vfx varchar(256) not null, awakening_sfx varchar(256) not null,",
        "    canon_source text not null, canon_confidence varchar(48) not null, status varchar(48) not null,",
        "    check (base_form_id <> awakened_form_id)",
        ");",
        "alter table hero_versions add constraint fk_hero_versions_awakening foreign key (awakening_id) references awakening_definitions(awakening_id);",
        "create table awakening_skill_definitions (",
        "    awakening_skill_id varchar(192) primary key, hero_id varchar(128) not null unique references hero_versions(hero_id) on delete cascade,",
        "    awakening_id varchar(192) not null unique references awakening_definitions(awakening_id) on delete cascade,",
        "    name_en varchar(192) not null, name_vi varchar(192) not null, classification varchar(48) not null, element varchar(32) not null,",
        "    damage_type varchar(64) not null, target_type varchar(64) not null, coefficient numeric(10,4) not null default 0,",
        "    chakra_cost integer not null default 0, cooldown integer not null default 0, effects text not null, statuses text not null default '',",
        "    buffs text not null default '', debuffs text not null default '', special_mechanic text not null,",
        "    animation_key varchar(256) not null, vfx_key varchar(256) not null, sfx_key varchar(256) not null,",
        "    description_en text not null, description_vi text not null, canon_source text not null, canon_confidence varchar(48) not null, status varchar(48) not null",
        ");",
        "create table awakening_visual_definitions (",
        "    awakening_id varchar(192) primary key references awakening_definitions(awakening_id) on delete cascade,",
        "    hero_id varchar(128) not null unique references hero_versions(hero_id) on delete cascade,",
        "    visual_spec jsonb not null, reference_source text not null, status varchar(48) not null",
        ");",
        "alter table player_heroes add column hero_version_id varchar(128);",
        "alter table player_heroes add column awakened boolean not null default false;",
        "alter table player_heroes add column awakened_at timestamptz;",
        "create index idx_player_heroes_hero_version on player_heroes(player_id, hero_version_id);",
        "",
    ]

    for h in heroes:
        vals = [h[k] for k in ["hero_id","character_id","version_name","display_name_en","display_name_vi","era","rarity","role","element","faction","base_form_id","basic_skill","skill_1","skill_2","ultimate","passive","awakening_id"]]
        vals += [str(h["summonable"]).lower() == "true", h["status"]]
        lines.append("insert into hero_versions (hero_id,character_id,version_name,display_name_en,display_name_vi,era,rarity,role,element,faction,base_form_id,basic_skill,skill_1,skill_2,ultimate,passive,awakening_id,summonable,status) values (" + ",".join(sql(v) for v in vals) + ");")
    lines.append("")
    for a in aliases:
        vals = [a[k] for k in ["skill_id","hero_id","slot","source_technique_id","name_en","name_vi","channel","kind","tags","explicitness","status","research_note"]]
        lines.append("insert into hero_version_skill_aliases (skill_id,hero_id,slot,source_technique_id,name_en,name_vi,channel,kind,tags,explicitness,status,research_note) values (" + ",".join(sql(v) for v in vals) + ");")
    lines.append("")
    # Circular hero_versions.awakening_id FK requires insert awakenings before setting hero awakening IDs.
    # Temporarily clear references created by hero inserts, then restore after awakening rows exist.
    lines.insert(lines.index(""), "") if False else None
    # hero inserts above would violate FK because awakenings do not yet exist. Generate them with awakening_id NULL first by rewriting lines.
    hero_insert_start = next(i for i, line in enumerate(lines) if line.startswith("insert into hero_versions"))
    for i in range(hero_insert_start, hero_insert_start + len(heroes)):
        # Replace the 17th value (awakening_id) safely by regenerating below is clearer; this line is overwritten after construction.
        pass
    # Rebuild hero insert block with NULL awakening_id and later update.
    before = lines[:hero_insert_start]
    after_alias_start = hero_insert_start + len(heroes) + 1
    alias_and_after = lines[after_alias_start:]
    rebuilt = []
    for h in heroes:
        vals = [h[k] for k in ["hero_id","character_id","version_name","display_name_en","display_name_vi","era","rarity","role","element","faction","base_form_id","basic_skill","skill_1","skill_2","ultimate","passive"]]
        vals += [None, str(h["summonable"]).lower() == "true", h["status"]]
        rebuilt.append("insert into hero_versions (hero_id,character_id,version_name,display_name_en,display_name_vi,era,rarity,role,element,faction,base_form_id,basic_skill,skill_1,skill_2,ultimate,passive,awakening_id,summonable,status) values (" + ",".join(sql(v) for v in vals) + ");")
    lines = before + rebuilt + [""] + alias_and_after

    for a in awakenings:
        vals = [a[k] for k in ["awakening_id","hero_id","base_form_id","awakened_form_id","name_en","name_vi","unlock_level","unlock_rank","awakening_skill_id","stat_modifier_profile","base_model","awakened_model","base_portrait","awakened_portrait","awakening_animation","awakening_vfx","awakening_sfx","canon_source","canon_confidence","status"]]
        lines.append("insert into awakening_definitions (awakening_id,hero_id,base_form_id,awakened_form_id,name_en,name_vi,unlock_level,unlock_rank,awakening_skill_id,stat_modifier_profile,base_model,awakened_model,base_portrait,awakened_portrait,awakening_animation,awakening_vfx,awakening_sfx,canon_source,canon_confidence,status) values (" + ",".join(sql(v) for v in vals) + ");")
    lines.append("")
    for h in heroes:
        if h["awakening_id"]:
            lines.append(f"update hero_versions set awakening_id={sql(h['awakening_id'])} where hero_id={sql(h['hero_id'])};")
    lines.append("")
    for s in awakening_skills:
        keys = ["awakening_skill_id","hero_id","awakening_id","name_en","name_vi","classification","element","damage_type","target_type","coefficient","chakra_cost","cooldown","effects","statuses","buffs","debuffs","special_mechanic","animation_key","vfx_key","sfx_key","description_en","description_vi","canon_source","canon_confidence","status"]
        lines.append("insert into awakening_skill_definitions (" + ",".join(keys) + ") values (" + ",".join(sql(s[k]) for k in keys) + ");")
    lines.append("")
    for v in visuals:
        # Keep detailed production visual spec in CSV; DB stores a compact identity/path envelope for runtime catalog migration.
        spec = "{\"bodyChange\":\"" + str(v["body_change"]).replace('\\','\\\\').replace('"','\\"') + "\",\"awakeningSkillVfx\":\"" + str(v["awakening_skill_vfx"]).replace('\\','\\\\').replace('"','\\"') + "\"}"
        lines.append("insert into awakening_visual_definitions (awakening_id,hero_id,visual_spec,reference_source,status) values (" + ",".join([sql(v["awakening_id"]),sql(v["hero_id"]),sql(spec)+"::jsonb",sql(v["reference_source"]),sql(v["status"])]) + ");")
    lines += [
        "",
        "-- M43 will backfill player_heroes.hero_version_id and then add the FK/not-null constraint after compatibility mapping is verified.",
        "-- The legacy frame_tier AWAKENING enum/check value remains untouched in M42 and is deprecated only after runtime cutover.",
    ]
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    raise SystemExit(main())
