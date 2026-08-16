#!/usr/bin/env python3
from __future__ import annotations

import csv
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]


def rows(path: str) -> list[dict[str, str]]:
    with (ROOT / path).open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(value: bool, message: str) -> None:
    if not value:
        raise ValueError(message)


def main() -> int:
    try:
        awakenings = rows("game-data/progression/awakenings.csv")
        visuals = rows("game-data/assets/awakening-visuals.csv")
        awakening_ids = {row["awakening_id"].strip() for row in awakenings}
        visual_ids = {row["awakening_id"].strip() for row in visuals}
        require(awakening_ids == visual_ids, f"Awakening visual coverage mismatch: awakenings={len(awakening_ids)} visuals={len(visual_ids)}")
        require(len(visual_ids) == len(visuals), "duplicate awakening_id in visual catalog")
        for row in visuals:
            require(row["hero_id"].strip(), f"{row['awakening_id']}: missing hero_id")
            for field in ("transition_start", "transition_mid", "transition_end", "basic_vfx_modifier", "ultimate_vfx_modifier", "awakening_skill_vfx", "camera_sequence", "screen_effect"):
                require(row[field].strip(), f"{row['awakening_id']}: missing {field}")
            require(row["status"].strip() == "ASSET_SPEC_PENDING_PRODUCTION", f"{row['awakening_id']}: M46 must not fake production READY art")

        ownership = text("server/src/main/java/com/ninjaassemble/hero/ownership/HeroOwnershipService.java")
        require("public boolean awaken(UUID playerId, UUID playerHeroId)" in ownership, "one-time awaken primitive missing")
        require("awakened = true" in ownership and "awakening_level = 1" in ownership, "Awakening must remain boolean/one-stage")

        controller = text("server/src/main/java/com/ninjaassemble/hero/awakening/HeroAwakeningController.java")
        for token in ("@GetMapping", "@PostMapping", "ownership.awaken", "AwakeningPresentationCatalogService"):
            require(token in controller, f"Awakening API missing {token}")

        participant = text("server/src/main/java/com/ninjaassemble/play/domain/BattleParticipant.java")
        for token in ("String heroId", "boolean awakened", "String awakeningId", "String presentationKey", "heroVersion("):
            require(token in participant, f"BattleParticipant missing Hero Version presentation field: {token}")

        campaign = text("server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java")
        arena = text("server/src/main/java/com/ninjaassemble/pvp/application/ArenaApplicationService.java")
        require("BattleParticipant.heroVersion(" in campaign, "Campaign participants must emit Hero Version Awakening identity")
        require("BattleParticipant.heroVersion(" in arena, "Arena participants must emit Hero Version Awakening identity")

        api = text("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs")
        require("GetAwakeningAsync" in api and "AwakenAsync" in api, "Unity API client missing Awakening calls")

        roster = text("client-unity/Assets/Scripts/Game/Heroes/RosterFormationPlayableBridge.cs")
        for forbidden in ("SelectNextVariant", "NEXT VARIANT", "GetVariantsAsync(hero.characterId)"):
            require(forbidden not in roster, f"Hero Detail still exposes legacy variant cycling: {forbidden}")
        for token in ("AwakenSelected", "GetAwakeningAsync", "AwakenAsync", "hero.heroId", "hero.awakened"):
            require(token in roster, f"Hero Detail Awakening UX missing {token}")

        dtos = text("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs")
        for token in ("public string heroId", "public bool awakened", "public string awakeningId", "public string presentationKey"):
            require(token in dtos, f"Unity DTO missing {token}")

        catalog = text("client-unity/Assets/Scripts/Game/Presentation/HeroArtRuntimeCatalog.cs")
        stage = text("client-unity/Assets/Scripts/Game/Presentation/BattleVisualStage.cs")
        actor = text("client-unity/Assets/Scripts/Game/Presentation/BattleActorView.cs")
        require("ResolveHeroVersion" in catalog and '"awakened" : "base"' in catalog, "Hero art resolver must separate base/awakened paths")
        require("ResolveHeroVersion(participant.heroId, participant.awakened" in stage, "Battle stage does not consume Hero Version Awakening identity")
        require("AwakeningId" in actor and "PresentationKey" in actor and "Awakened" in actor, "BattleActorView is missing Awakening identity")

        print(f"AWAKENING_UX_PRESENTATION_OK awakenings={len(awakening_ids)} visual_specs={len(visuals)}")
        return 0
    except (ValueError, KeyError) as error:
        print(f"AWAKENING_UX_PRESENTATION_INVALID {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
