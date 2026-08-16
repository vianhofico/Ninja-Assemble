#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
HEROES = ROOT / "game-data/heroes/heroes.csv"
SHOP = ROOT / "game-data/shop/shop-offers.csv"
SYNERGY = ROOT / "game-data/synergy/team-synergies.csv"
BALANCE = ROOT / "game-data/reference/balance-profiles.csv"
SUMMON_SAMPLES = ROOT / "game-data/reference/measurements/summon-samples.csv"


def rows(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def main() -> int:
    try:
        heroes = rows(HEROES)
        require(bool(heroes), "heroes.csv must not be empty")
        hero_ids = [row["hero_id"].strip() for row in heroes]
        require(len(hero_ids) == len(set(hero_ids)), "duplicate hero_id in heroes.csv")
        valid_rarities = {"R", "SR", "SSR", "UR"}
        summonable = [row for row in heroes if row["summonable"].strip().lower() == "true"]
        require(bool(summonable), "no summonable Hero Versions")
        for row in summonable:
            require(row["rarity"].strip() in valid_rarities, f"{row['hero_id']}: invalid summon rarity")
            require("::" not in row["hero_id"], f"{row['hero_id']}: legacy variant identity leaked into Hero Version id")

        awakening_ids = {row["awakening_id"].strip() for row in heroes if row["awakening_id"].strip()}
        require(not (set(hero_ids) & awakening_ids), "Awakening id collides with collectible Hero Version id")

        pool = text("server/src/main/java/com/ninjaassemble/play/application/CompleteRosterBannerFactory.java")
        require("HeroVersionAcquisitionCatalogService" in pool, "summon pool must use Hero Version acquisition catalog")
        require("catalog.summonable()" in pool, "summon pool must filter by summonable Hero Versions")
        require("VariantCatalogService" not in pool, "legacy VariantCatalogService must not drive summon pool")
        require("rarityFor(" not in pool, "rarity must come from Hero Version data, not variant-name heuristics")

        entry = text("server/src/main/java/com/ninjaassemble/summon/domain/SummonPoolEntry.java")
        require("String heroId" in entry, "SummonPoolEntry must use heroId")
        require("heroId.contains(\"::\")" in entry, "SummonPoolEntry must reject legacy character::variant identity")
        require("heroVariantId" not in entry, "legacy heroVariantId field remains in SummonPoolEntry")

        service = text("server/src/main/java/com/ninjaassemble/play/application/SummonApplicationService.java")
        for token in ("grantHeroVersion", "hero_version_id", "pulled.entry().heroId()"):
            require(token in service, f"summon service missing Hero Version token: {token}")
        for forbidden in ("unlockVariant(", "heroVariantId()", "split(\"::\""):
            require(forbidden not in service, f"summon service still contains legacy acquisition path: {forbidden}")

        migration = text("server/src/main/resources/db/migration/V12__hero_version_acquisition.sql")
        for token in (
            "rename column hero_variant_id to legacy_hero_variant_id",
            "add column hero_version_id",
            "legacy_variant_hero_version_map",
            "ck_summon_history_identity",
        ):
            require(token in migration, f"V12 missing migration invariant: {token}")

        shops = rows(SHOP)
        require(bool(shops), "shop catalog must not be empty")
        require("hero_id" not in (shops[0].keys() if shops else []), "current item shop unexpectedly exposes hero_id acquisition")
        require(all(row.get("item_id", "").strip() for row in shops), "shop offer missing item_id")

        synergies = rows(SYNERGY)
        require(bool(synergies), "synergy catalog must not be empty")
        require(all(row.get("characters", "").strip() for row in synergies), "synergy rows must remain character-identity based")
        synergy_definition = text("server/src/main/java/com/ninjaassemble/synergy/domain/TeamSynergyDefinition.java")
        synergy_evaluator = text("server/src/main/java/com/ninjaassemble/synergy/domain/TeamSynergyEvaluator.java")
        require("requiredCharacterIds" in synergy_definition, "synergy must explicitly use character identity")
        require("Set<String> teamCharacterIds" in synergy_evaluator, "synergy evaluator must de-duplicate character identity")

        balance = {row["profile_id"].strip(): row for row in rows(BALANCE)}
        profile_id = "complete-roster-hero-version-experimental-v2"
        require(profile_id in balance, "Hero Version summon evidence profile missing")
        require(balance[profile_id]["status"].strip() == "EXPERIMENTAL", "summon profile must remain EXPERIMENTAL until evidence exists")

        with SUMMON_SAMPLES.open(encoding="utf-8-sig", newline="") as handle:
            fields = next(csv.reader(handle))
        require("hero_id" in fields, "summon evidence schema must use hero_id")
        require("variant" not in fields, "summon evidence schema must not use variant")

        print(
            "HERO_VERSION_ACQUISITION_OK "
            f"heroes={len(heroes)} summonable={len(summonable)} awakenings={len(awakening_ids)} "
            f"shops={len(shops)} synergies={len(synergies)}"
        )
        return 0
    except (ValueError, KeyError) as error:
        print(f"HERO_VERSION_ACQUISITION_INVALID {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
