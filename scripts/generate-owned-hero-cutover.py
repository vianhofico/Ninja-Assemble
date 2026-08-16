#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "game-data" / "design"
REF = ROOT / "game-data" / "reference"
MIGRATION_DIR = ROOT / "game-data" / "migration"
SQL_PATH = ROOT / "server" / "src" / "main" / "resources" / "db" / "migration" / "V11__owned_hero_version_cutover.sql"


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


def sql(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "true" if value else "false"
    return "'" + str(value).replace("'", "''") + "'"


def source_rows() -> tuple[list[dict[str, str]], dict[tuple[str, str], int]]:
    rows: list[dict[str, str]] = []
    order: dict[tuple[str, str], int] = {}
    seen: set[tuple[str, str]] = set()
    per_char: defaultdict[str, int] = defaultdict(int)
    for path in sorted(REF.glob("variant-census*.csv")):
        for row in read(path):
            key = (row["character_id"].strip(), row["variant"].strip())
            if key in seen:
                continue
            seen.add(key)
            order[key] = per_char[key[0]]
            per_char[key[0]] += 1
            rows.append({"character_id": key[0], "variant": key[1]})
    return rows, order


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    source, order = source_rows()
    variants = read(DESIGN / "variant-reclassification.csv")
    pairs = read(DESIGN / "hero-awakening-pairs.csv")
    forms = {r["form_id"].strip(): r for r in read(DESIGN / "character-form-pool.csv")}

    pair_by_awakening_id = {
        f"awakening-{p['hero_id'].strip()}": p
        for p in pairs if p["awakening_form_id"].strip()
    }
    pairs_by_char: defaultdict[str, list[dict[str, str]]] = defaultdict(list)
    for pair in pairs:
        pairs_by_char[pair["character_id"].strip()].append(pair)

    primary: dict[str, str] = {}
    for character_id, char_pairs in pairs_by_char.items():
        ranked: list[tuple[int, str]] = []
        for pair in char_pairs:
            form = forms.get(pair["base_form_id"].strip())
            variant = form["form_name"].strip() if form else "Base"
            rank = order.get((character_id, variant), 1_000_000)
            ranked.append((rank, pair["hero_id"].strip()))
        ranked.sort(key=lambda x: (x[0], x[1]))
        primary[character_id] = ranked[0][1]

    rows: list[dict[str, object]] = []
    seen_keys: set[tuple[str, str]] = set()

    # Compatibility entries for null/BASE legacy selection only exist for normal collectible characters.
    for character_id, hero_id in sorted(primary.items()):
        for legacy_variant_id in ("__BASE__", "BASE"):
            rows.append({
                "legacy_character_id": character_id,
                "legacy_variant_id": legacy_variant_id,
                "hero_version_id": hero_id,
                "awakened": "false",
                "mapping_kind": "PRIMARY_COMPATIBILITY",
                "reason": "Legacy null/BASE ownership resolves to the earliest approved collectible Hero Version for the character.",
            })
            seen_keys.add((character_id, legacy_variant_id))

    no_hero_count = 0
    for row in variants:
        character_id = row["character_id"].strip()
        variant = row["old_variant"].strip()
        classification = row["classification"].strip()
        hero_id = ""
        awakened = False
        kind = "COMPATIBILITY_MERGE"
        if classification == "COLLECTIBLE_HERO_VERSION":
            hero_id = row["new_hero_id"].strip()
            kind = "COLLECTIBLE_HERO_VERSION"
        elif classification == "AWAKENING_FORM":
            awakening_id = row["new_awakening_form_id"].strip()
            pair = pair_by_awakening_id.get(awakening_id)
            if pair is None:
                raise SystemExit(f"missing pair owner for Awakening mapping {character_id}/{variant}: {awakening_id}")
            hero_id = pair["hero_id"].strip()
            awakened = True
            kind = "AWAKENING_FORM"
        else:
            hero_id = primary.get(character_id, "")
            if hero_id:
                kind = f"{classification}_TO_PRIMARY"
            else:
                # SPECIAL_INDEPENDENT_CHARACTER / summon-only source content deliberately has no Hero Version.
                # It stays in the 427-row audit bridge, but V11 will not silently turn it into a ninja hero.
                kind = f"{classification}_NO_HERO_VERSION"
                no_hero_count += 1
        if not hero_id and classification in {"COLLECTIBLE_HERO_VERSION", "AWAKENING_FORM"}:
            raise SystemExit(f"collectible/Awakening source cannot resolve to Hero Version: {character_id}/{variant}")
        key = (character_id, variant)
        if key in seen_keys:
            raise SystemExit(f"duplicate legacy mapping key {key}")
        seen_keys.add(key)
        rows.append({
            "legacy_character_id": character_id,
            "legacy_variant_id": variant,
            "hero_version_id": hero_id,
            "awakened": str(awakened).lower(),
            "mapping_kind": kind,
            "reason": row["reason"].strip(),
        })

    source_keys = {(r["character_id"], r["variant"]) for r in source}
    mapped_source = {(str(r["legacy_character_id"]), str(r["legacy_variant_id"])) for r in rows
                     if str(r["legacy_variant_id"]) not in {"__BASE__", "BASE"}}
    if source_keys != mapped_source or len(source_keys) != 427:
        raise SystemExit(f"legacy ownership mapping coverage mismatch source={len(source_keys)} mapped={len(mapped_source)}")

    hero_ids = {p["hero_id"].strip() for p in pairs}
    unknown = {str(r["hero_version_id"]) for r in rows if str(r["hero_version_id"]).strip()} - hero_ids
    if unknown:
        raise SystemExit(f"legacy mappings reference unknown Hero Versions: {sorted(unknown)[:10]}")

    if args.write:
        write(MIGRATION_DIR / "legacy-hero-version-map.csv", [
            "legacy_character_id", "legacy_variant_id", "hero_version_id", "awakened", "mapping_kind", "reason"
        ], rows)
        SQL_PATH.parent.mkdir(parents=True, exist_ok=True)
        SQL_PATH.write_text(render_sql(rows), encoding="utf-8")

    print(
        f"OWNED_HERO_CUTOVER_MAP_OK source_variants={len(source_keys)} mapping_rows={len(rows)} "
        f"hero_versions={len(hero_ids)} no_hero_version_rows={no_hero_count}"
    )
    return 0


def render_sql(rows: list[dict[str, object]]) -> str:
    lines = [
        "-- Generated by scripts/generate-owned-hero-cutover.py. Do not hand-edit.",
        "-- M43 converts legacy character+variant ownership into collectible Hero Version ownership + one boolean Awakening.",
        "-- Special independent/summon-only content is audited here but deliberately not fabricated as a Hero Version.",
        "",
        "create table legacy_variant_hero_version_map (",
        "    legacy_character_id varchar(128) not null,",
        "    legacy_variant_id varchar(192) not null,",
        "    hero_version_id varchar(128) references hero_versions(hero_id),",
        "    awakened boolean not null default false,",
        "    mapping_kind varchar(96) not null,",
        "    reason text not null,",
        "    primary key (legacy_character_id, legacy_variant_id),",
        "    check (not awakened or mapping_kind = 'AWAKENING_FORM'),",
        "    check (hero_version_id is not null or mapping_kind like '%_NO_HERO_VERSION')",
        ");",
        "",
    ]
    for row in rows:
        hero_version_id = str(row["hero_version_id"]).strip() or None
        vals = [row["legacy_character_id"], row["legacy_variant_id"], hero_version_id]
        vals += [str(row["awakened"]).lower() == "true", row["mapping_kind"], row["reason"]]
        lines.append(
            "insert into legacy_variant_hero_version_map "
            "(legacy_character_id,legacy_variant_id,hero_version_id,awakened,mapping_kind,reason) values ("
            + ",".join(sql(v) for v in vals) + ");"
        )

    lines += [
        "",
        "-- Drop the old one-row-per-character uniqueness so multiple approved Hero Versions of Naruto/Sasuke/etc. can coexist.",
        "alter table player_heroes drop constraint if exists player_heroes_player_id_hero_definition_id_key;",
        "",
        "-- Backfill each existing normal-hero ownership row from its selected legacy variant.",
        "update player_heroes ph",
        "set hero_version_id = map.hero_version_id,",
        "    awakened = map.awakened,",
        "    awakened_at = case when map.awakened then coalesce(ph.awakened_at, now()) else ph.awakened_at end,",
        "    awakening_level = case when map.awakened then 1 else 0 end",
        "from legacy_variant_hero_version_map map",
        "where map.legacy_character_id = ph.hero_definition_id",
        "  and map.hero_version_id is not null",
        "  and map.legacy_variant_id = case",
        "      when ph.current_variant_id is null or btrim(ph.current_variant_id) = '' or upper(ph.current_variant_id) = 'BASE' then '__BASE__'",
        "      else ph.current_variant_id",
        "  end;",
        "",
        "-- If an already-unlocked legacy Awakening belongs to the row's Hero Version, preserve it as awakened=true.",
        "update player_heroes ph",
        "set awakened = true, awakened_at = coalesce(ph.awakened_at, now()), awakening_level = 1",
        "where exists (",
        "    select 1 from hero_variant_unlocks u",
        "    join legacy_variant_hero_version_map map",
        "      on map.legacy_character_id = ph.hero_definition_id and map.legacy_variant_id = u.variant_id",
        "    where u.player_hero_id = ph.id and map.hero_version_id is not null",
        "      and map.hero_version_id = ph.hero_version_id and map.awakened = true",
        ");",
        "",
        "-- Materialize every independently collectible legacy-unlocked Hero Version as its own ownership row.",
        "insert into player_heroes(",
        "    id, player_id, hero_definition_id, level, exp, frame_tier, frame_advance_step, tailed_beast_state, skill_state, created_at,",
        "    frame_plus, current_variant_id, awakening_level, transformation_state, scroll_state, hero_version_id, awakened, awakened_at",
        ")",
        "select gen_random_uuid(), x.player_id, x.hero_definition_id, x.level, x.exp, x.frame_tier, x.frame_advance_step,",
        "       x.tailed_beast_state, x.skill_state, now(), x.frame_plus, null, case when x.awakened then 1 else 0 end,",
        "       x.transformation_state, x.scroll_state, x.hero_version_id, x.awakened, case when x.awakened then now() else null end",
        "from (",
        "    select distinct on (ph.player_id, map.hero_version_id)",
        "           ph.player_id, ph.hero_definition_id, ph.level, ph.exp, ph.frame_tier, ph.frame_advance_step,",
        "           ph.tailed_beast_state, ph.skill_state, ph.frame_plus, ph.transformation_state, ph.scroll_state,",
        "           map.hero_version_id, bool_or(map.awakened) over (partition by ph.player_id, map.hero_version_id) as awakened",
        "    from hero_variant_unlocks u",
        "    join player_heroes ph on ph.id = u.player_hero_id",
        "    join legacy_variant_hero_version_map map",
        "      on map.legacy_character_id = ph.hero_definition_id and map.legacy_variant_id = u.variant_id",
        "    where map.hero_version_id is not null and map.hero_version_id <> ph.hero_version_id",
        "    order by ph.player_id, map.hero_version_id, map.awakened desc",
        ") x",
        "where not exists (",
        "    select 1 from player_heroes owned",
        "    where owned.player_id = x.player_id and owned.hero_version_id = x.hero_version_id",
        ");",
        "",
        "-- Refuse silent data loss: any legacy player_hero that was actually a special/summon-only entity must be migrated explicitly later.",
        "do $$",
        "begin",
        "    if exists (select 1 from player_heroes where hero_version_id is null) then",
        "        raise exception 'M43 cutover found player_heroes with no collectible Hero Version mapping (likely SPECIAL_INDEPENDENT_CHARACTER content)';",
        "    end if;",
        "end $$;",
        "",
        "alter table player_heroes alter column hero_version_id set not null;",
        "alter table player_heroes add constraint fk_player_heroes_hero_version foreign key (hero_version_id) references hero_versions(hero_id);",
        "create unique index uq_player_heroes_player_hero_version on player_heroes(player_id, hero_version_id);",
        "create index idx_player_heroes_awakened on player_heroes(player_id, awakened) where awakened = true;",
        "",
        "comment on column player_heroes.hero_definition_id is 'DEPRECATED compatibility character id; use hero_version_id for collectible identity.';",
        "comment on column player_heroes.current_variant_id is 'DEPRECATED legacy variant selector; one-Awakening runtime uses awakened boolean.';",
        "comment on column player_heroes.awakening_level is 'DEPRECATED compatibility mirror: 0=normal, 1=awakened. No multi-stage Awakening.';",
        "",
    ]
    return "\n".join(lines) + "\n"


if __name__ == "__main__":
    raise SystemExit(main())
