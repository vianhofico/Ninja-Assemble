#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import re
import unicodedata
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REFERENCE = ROOT / "game-data" / "reference"
DESIGN = ROOT / "game-data" / "design"
DOCS = ROOT / "docs" / "design"
SKILLS = ROOT / "game-data" / "skills"

CLASSIFICATIONS = {
    "COLLECTIBLE_HERO_VERSION",
    "AWAKENING_FORM",
    "SKILL_OR_ULTIMATE",
    "TEMPORARY_COMBAT_FORM",
    "COSMETIC_SKIN",
    "COOPERATION_FORM_OR_TECHNIQUE",
    "SPECIAL_INDEPENDENT_CHARACTER",
    "MERGED_OR_REMOVED_DUPLICATE",
}


def slug(value: str) -> str:
    text = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode("ascii")
    return re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")


def form_id(character_id: str, variant: str) -> str:
    return f"form-{character_id}-{slug(variant)}"


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def write_csv(path: Path, fields: list[str], rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow({k: row.get(k, "") for k in fields})


@dataclass(frozen=True)
class Pair:
    hero_id: str
    character_id: str
    hero_version_name: str
    base_variant: str
    awakening_variant: str | None
    awakening_name: str
    hero_score: int
    pair_score: int | None
    role: str
    era: str
    awakening_type: str
    canon_confidence: str
    source_reference: str
    identity: str
    awakening_skill_concept: str
    reason: str


def P(hero_id: str, character_id: str, hero_version_name: str, base_variant: str,
      awakening_variant: str | None, awakening_name: str, hero_score: int,
      pair_score: int | None, role: str, era: str, awakening_type: str,
      confidence: str, source: str, identity: str, awakening_skill: str, reason: str) -> Pair:
    return Pair(hero_id, character_id, hero_version_name, base_variant, awakening_variant,
                awakening_name, hero_score, pair_score, role, era, awakening_type,
                confidence, source, identity, awakening_skill, reason)

NARUTO = "https://naruto.fandom.com/wiki/Naruto_Uzumaki"
SASUKE = "https://naruto.fandom.com/wiki/Sasuke_Uchiha"
JINCHURIKI = "https://naruto.fandom.com/wiki/Jinch%C5%ABriki_Forms"
EIGHT_GATES = "https://naruto.fandom.com/wiki/Eight_Gates"
CURSED_SEAL = "https://naruto.fandom.com/wiki/Cursed_Seal"
SAGE_MODE = "https://naruto.fandom.com/wiki/Sage_Mode"
RINNEGAN = "https://naruto.fandom.com/wiki/Rinnegan"

# Curated hero-version boundaries. These are proposal rows, not production data.
# Major characters intentionally receive multiple versions only where their combat identity changes.
PAIRS: list[Pair] = [
    P("naruto-genin", "naruto-uzumaki", "Naruto [Genin]", "Genin", "One-Tail Cloak", "One-Tail Naruto", 88, 91, "HYBRID", "PART_I", "JINCHURIKI", "MANGA_CANON", NARUTO,
      "shadow clones; early taijutsu; Rasengan; unpredictable tempo; Kurama pressure passive", "one-tail chakra assault", "Part I Naruto has a self-contained early kit and a visually obvious first jinchuriki escalation."),
    P("naruto-sage", "naruto-uzumaki", "Naruto [Sage Mode]", "Sage Mode", "KCM2", "Kurama Link Naruto", 94, 92, "BURST_DPS", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", NARUTO + ";" + SAGE_MODE,
      "Frog Kata; Sage sensing; Rasengan family; Rasenshuriken; natural-energy management", "Kurama avatar combo", "Sage Naruto and controlled Kurama Naruto have distinct visual/combat identities while KCM1 can remain a temporary transition state."),
    P("naruto-six-paths", "naruto-uzumaki", "Naruto [Six Paths]", "Six Paths Sage Mode", "Asura Kurama Mode", "Asura Kurama Mode", 98, 96, "UTILITY_DPS", "WAR_ARC", "SIX_PATHS", "MANGA_CANON", NARUTO,
      "Six Paths sensory utility; Truth-Seeking Ball interaction; tailed-beast Rasenshuriken; aerial mobility; ally protection", "Six Paths: Ultra-Big Ball Rasenshuriken", "Asura Kurama Mode is a form-specific climax and should be reserved for the single Awakening instead of being another summonable Naruto."),
    P("naruto-hokage", "naruto-uzumaki", "Naruto [Hokage]", "Hokage", "Baryon Mode", "Baryon Mode", 92, 88, "BRUISER", "ADULT", "SPECIAL_MODE", "MANGA_CANON", NARUTO,
      "adult clone mastery; compact Rasengan pressure; chakra arms; leadership support; high efficiency", "Baryon life-drain rush", "Adult Naruto is a separate era identity; Baryon Mode is reserved as its one late-era Awakening rather than another collectible form."),

    P("sasuke-genin", "sasuke-uchiha", "Sasuke [Genin]", "Genin", "Curse Mark Level 2", "Curse Mark Level 2", 88, 91, "ASSASSIN", "PART_I", "CURSED_SEAL", "MANGA_CANON", SASUKE,
      "Fireball; shuriken wire; Lion Combo; Chidori; Sharingan prediction", "black Chidori dive", "Part I Sasuke has a distinct physical/shuriken/Chidori loop and a canonical visible Curse Mark escalation."),
    P("sasuke-hebi", "sasuke-uchiha", "Sasuke [Hebi]", "Hebi", "Mangekyo Sharingan", "Mangekyo Sasuke", 95, 90, "ASSASSIN", "HEBI_TAKA", "DOJUTSU", "MANGA_CANON", SASUKE,
      "kenjutsu; Chidori variants; snake summons; Kirin setup; tactical lightning", "Amaterasu ignition", "Hebi Sasuke is sword/snake/lightning focused; Mangekyo introduces a clear new ocular combat identity."),
    P("sasuke-ems", "sasuke-uchiha", "Sasuke [Eternal Mangekyo]", "Eternal Mangekyo Sharingan", "Rinnegan", "Rinnegan Sasuke", 98, 95, "CONTROL_DPS", "WAR_ARC", "SIX_PATHS_DOJUTSU", "MANGA_CANON", SASUKE + ";" + RINNEGAN,
      "Amaterasu control; Susanoo pressure; Flame Control; high-end Chidori; defensive ocular play", "Amenotejikara execution", "EMS and Rinnegan/Six Paths Sasuke differ strongly in space-time control and battlefield manipulation."),
    P("sasuke-adult", "sasuke-uchiha", "Sasuke [Adult]", "Adult", None, "", 90, None, "CONTROL_DPS", "ADULT", "NONE", "MANGA_CANON", SASUKE,
      "space-time scouting; sword/one-hand seals; Amenotejikara; precision lightning; dimensional utility", "", "Adult Sasuke is a distinct era hero, but the census has no canonically stronger persistent solo form that should be invented as an Awakening."),

    P("sakura-genin", "sakura-haruno", "Sakura [Genin]", "Genin", None, "", 68, None, "SUPPORT", "PART_I", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Sakura_Haruno",
      "chakra control; substitution; trap reading; team support; early genjutsu resistance", "", "The early version is recognizable but has no strong canonical transformation to force into an Awakening."),
    P("sakura-shippuden", "sakura-haruno", "Sakura [Shippuden]", "Shippuden", "Byakugo", "Byakugo Sakura", 91, 96, "HEALER_BRUISER", "SHIPPUDEN", "SEAL_RELEASE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Sakura_Haruno",
      "chakra-enhanced strength; medical ninjutsu; cleanse; Katsuyu support; reserve-chakra passive", "Hundred Healings surge", "Byakugo is an ideal one-step Awakening: visible seal change, major power increase, and natural sixth-skill identity."),

    P("kakashi-young", "kakashi-hatake", "Kakashi [Young]", "Young", "Sharingan", "Sharingan Kakashi", 82, 88, "ASSASSIN", "KANNABI", "DOJUTSU", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kakashi_Hatake",
      "White Light Chakra Sabre; Chidori; tactical substitutions; team command; speed", "Sharingan counter", "Young Kakashi has a canonical, visually immediate transition when he receives Obito's Sharingan."),
    P("kakashi-anbu", "kakashi-hatake", "Kakashi [ANBU]", "ANBU", None, "", 78, None, "ASSASSIN", "ANBU", "NONE", "ANIME_CANON", "https://naruto.fandom.com/wiki/Kakashi_Hatake",
      "silent assassination; Lightning Blade; clone feints; tracking; ANBU tactics", "", "ANBU Kakashi is a distinct presentation/role, but a separate invented transformation is not required."),
    P("kakashi-war", "kakashi-hatake", "Kakashi [Sharingan]", "Mangekyo Sharingan", "Double Mangekyo", "Double Mangekyo Kakashi", 94, 94, "CONTROL_DPS", "WAR_ARC", "DOJUTSU", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kakashi_Hatake",
      "Kamui control; Lightning Blade; clone tactics; copy utility; precision defense", "Kamui Shuriken", "DMS is temporary in canon but is an exceptional visible high-level Awakening candidate for the war-era Kakashi combat identity."),

    P("itachi-anbu", "itachi-uchiha", "Itachi [ANBU]", "ANBU", "Mangekyo", "Mangekyo Itachi", 84, 88, "CONTROL", "ANBU", "DOJUTSU", "MANGA_CANON", "https://naruto.fandom.com/wiki/Itachi_Uchiha",
      "shuriken mastery; clone feints; fire style; subtle genjutsu; assassination", "Tsukuyomi lock", "ANBU Itachi and Mangekyo Itachi have a clear narrative and combat escalation."),
    P("itachi-akatsuki", "itachi-uchiha", "Itachi [Akatsuki]", "Akatsuki", "Susanoo", "Susanoo Itachi", 96, 95, "CONTROL_DPS", "AKATSUKI", "SUSANOO", "MANGA_CANON", "https://naruto.fandom.com/wiki/Itachi_Uchiha",
      "Tsukuyomi; Amaterasu; clone traps; fire style; ocular defensive passive", "Totsuka Blade sealing", "Susanoo supplies the strongest visible one-time Awakening and unique sixth skill without making another Itachi collectible."),

    P("madara-ems", "madara-uchiha", "Madara [Eternal Mangekyo]", "Eternal Mangekyo", None, "", 96, None, "DPS_TANK", "WARRING_STATES", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Madara_Uchiha",
      "gunbai control; fire style; Sharingan prediction; Susanoo pressure; battlefield dominance", "", "The census lacks a clean reserved persistent form between EMS and Rinnegan; this hero remains unawakened rather than inventing one."),
    P("madara-rinnegan", "madara-uchiha", "Madara [Rinnegan]", "Rinnegan", "Ten-Tails Jinchuriki", "Ten-Tails Madara", 99, 98, "BOSS_DPS", "WAR_ARC", "JINCHURIKI", "MANGA_CANON", "https://naruto.fandom.com/wiki/Madara_Uchiha",
      "Limbo; meteor pressure; Rinnegan control; Susanoo; absorption", "Truth-Seeking Orb: Limbo collapse", "Rinnegan Madara to Ten-Tails jinchuriki is a canonical, highly visible, mechanically distinct escalation."),

    P("obito-young", "obito-uchiha", "Obito [Young]", "Young", "Mangekyo", "Mangekyo Obito", 79, 87, "ASSASSIN", "KANNABI", "DOJUTSU", "MANGA_CANON", "https://naruto.fandom.com/wiki/Obito_Uchiha",
      "fire style; wire/shuriken; team assist; Sharingan awakening; desperation passive", "Kamui phase", "The Mangekyo awakening is tied directly to Obito's youth-era turning point and changes his combat identity."),
    P("obito-masked", "obito-uchiha", "Obito [Masked Man]", "Masked Man", None, "", 92, None, "CONTROL", "MASKED_MAN", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Obito_Uchiha",
      "Kamui intangibility; chains; genjutsu; teleport reposition; summon pressure", "", "Masked Man already has a complete standalone identity; White Mask is reserved for a separate late-war hero boundary."),
    P("obito-white-mask", "obito-uchiha", "Obito [White Mask]", "White Mask", "Ten-Tails Jinchuriki", "Ten-Tails Obito", 96, 98, "CONTROL_TANK", "WAR_ARC", "JINCHURIKI", "MANGA_CANON", "https://naruto.fandom.com/wiki/Obito_Uchiha",
      "Rinnegan jinchuriki paths; Kamui; chakra receivers; fan defense; tailed-beast control", "Truth-Seeking barrier collapse", "White Mask Obito has a distinct war kit and reserves Ten-Tails jinchuriki as the single visible Awakening."),

    P("minato-jonin", "minato-namikaze", "Minato [Jonin]", "Jonin", None, "", 85, None, "ASSASSIN_SUPPORT", "KANNABI", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Minato_Namikaze",
      "Flying Thunder God marks; kunai routing; Rasengan; rescue teleport; reaction passive", "", "Jonin Minato is a complete iconic kit; Fourth Hokage is primarily title/appearance and is not forced into an Awakening."),
    P("minato-edo", "minato-namikaze", "Minato [Reanimated]", "Edo Tensei", "KCM", "KCM Minato", 90, 91, "ASSASSIN_SUPPORT", "WAR_ARC", "JINCHURIKI", "MANGA_CANON", "https://naruto.fandom.com/wiki/Minato_Namikaze",
      "Flying Thunder God network; barrier transfer; Rasengan; battlefield teleport; reanimation sustain", "Kurama chakra teleport assault", "KCM is only available to Minato in the reanimated war context, making Edo Minato a justified separate hero/version boundary."),

    P("hashirama-hokage", "hashirama-senju", "Hashirama [First Hokage]", "First Hokage", "Sage Mode", "Sage Hashirama", 96, 94, "TANK_CONTROL", "HOKAGE", "SAGE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Hashirama_Senju",
      "Wood Dragon; forest control; regeneration; suppression; giant construct", "True Several Thousand Hands", "Sage Mode visibly marks and amplifies Hashirama while preserving his wood-style identity."),
    P("tobirama-hokage", "tobirama-senju", "Tobirama [Second Hokage]", "Second Hokage", None, "", 91, None, "ASSASSIN_CONTROL", "HOKAGE", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Tobirama_Senju",
      "Flying Thunder God; Water Release; sensing; tandem paper bomb; tactical control", "", "No higher persistent canon form is needed; Edo is treated separately as a skin/state."),
    P("hiruzen-prime", "hiruzen-sarutobi", "Hiruzen [Prime]", "Prime", None, "", 88, None, "FLEX", "HOKAGE", "NONE", "DESIGN_INTERPRETATION", "https://naruto.fandom.com/wiki/Hiruzen_Sarutobi",
      "five-nature mastery; Enma; sealing; shuriken; adaptive counterplay", "", "Prime is kept as the gameplay identity; title/Edo variants do not justify a forced transformation."),

    P("gaara-genin", "gaara", "Gaara [Genin]", "Genin", "Shukaku Cloak", "Shukaku Gaara", 89, 90, "TANK_CONTROL", "PART_I", "JINCHURIKI", "MANGA_CANON", "https://naruto.fandom.com/wiki/Gaara",
      "automatic sand defense; sand coffin; sand armor; terrain control; rage passive", "partial Shukaku assault", "Part I Gaara has a canonical visible partial-tailed-beast escalation."),
    P("gaara-kazekage", "gaara", "Gaara [Kazekage]", "Kazekage", None, "", 91, None, "TANK_CONTROL", "SHIPPUDEN", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Gaara",
      "large-scale sand defense; ally shielding; sand mausoleum; aerial control; rescue utility", "", "Kazekage Gaara is a separate mature combat identity; War Arc is retained as cosmetic/era material."),

    P("killer-b-base", "killer-b", "Killer B", "Base", "Eight-Tails Cloak", "Eight-Tails Cloak", 93, 92, "BRUISER", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "seven-sword style; lightning blades; lariat; chakra ink; jinchuriki counterplay", "Eight-Tails chakra lariat", "The cloak visibly awakens B while Full Gyuki remains a temporary/Ultimate-scale manifestation."),
    P("yagura-base", "yagura-karatachi", "Yagura", "Mizukage", "Three-Tails Cloak", "Three-Tails Cloak", 83, 86, "CONTROL", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "water mirror; staff combat; coral pressure; tailed-beast chakra; defensive control", "Three-Tails coral surge", "Canonical jinchuriki cloak offers the clearest single Awakening."),
    P("yugito-base", "yugito-nii", "Yugito Nii", "Base", "Two-Tails Cloak", "Two-Tails Cloak", 80, 86, "DPS", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "claw taijutsu; fire chakra; agility; pressure; jinchuriki passive", "Matatabi flame rush", "One cloak Awakening captures the jinchuriki escalation without making every beast state collectible."),
    P("roshi-base", "roshi", "Roshi", "Base", "Four-Tails Cloak", "Four-Tails Cloak", 80, 86, "BRUISER", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "lava release; armor pressure; close combat; burn; jinchuriki passive", "Lava chakra rampage", "Canonical cloak is reserved as the one persistent Awakening."),
    P("han-base", "han", "Han", "Base", "Five-Tails Cloak", "Five-Tails Cloak", 80, 86, "BRUISER", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "steam armor; horn rush; strength burst; pressure; jinchuriki passive", "Boil Release overdrive", "Canonical cloak is reserved as the one Awakening."),
    P("utakata-base", "utakata", "Utakata", "Base", "Six-Tails Cloak", "Six-Tails Cloak", 80, 85, "CONTROL", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "bubble ninjutsu; traps; acid pressure; evade; jinchuriki passive", "Saiken corrosive wave", "Canonical cloak is the strongest reusable visible transformation in the census."),
    P("fu-base", "fu", "Fu", "Base", "Seven-Tails Cloak", "Seven-Tails Cloak", 80, 85, "MOBILITY_DPS", "SHIPPUDEN", "JINCHURIKI", "MANGA_CANON", JINCHURIKI,
      "flight; scale powder; insect-like mobility; flash control; jinchuriki passive", "Chomei wing burst", "Canonical cloak creates an immediate visual and mechanical awakening."),

    P("orochimaru-sannin", "orochimaru", "Orochimaru [Sannin]", "Sannin", "White Snake", "White Snake Orochimaru", 94, 88, "CONTROL_SUMMONER", "SANNIN", "BODY_TRANSFORMATION", "MANGA_CANON", "https://naruto.fandom.com/wiki/Orochimaru",
      "snake summons; body substitution; binding; poison; forbidden-technique utility", "white snake rebirth", "The true white-snake body is a canonical visible transformation while era labels remain skins."),
    P("kabuto-part1", "kabuto-yakushi", "Kabuto [Part I]", "Part I", None, "", 76, None, "SUPPORT_ASSASSIN", "PART_I", "NONE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kabuto_Yakushi",
      "chakra scalpel; medical feints; nerve disruption; spy utility; recovery", "", "Part I Kabuto has a distinct spy/medical identity but no reserved transformation is forced."),
    P("kabuto-shippuden", "kabuto-yakushi", "Kabuto [Shippuden]", "Shippuden", "Snake Sage", "Snake Sage Kabuto", 92, 96, "CONTROL_DPS", "WAR_ARC", "SAGE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kabuto_Yakushi",
      "medical body mods; snake control; corpse utility; sensory control; regeneration", "Sage Art: Inorganic Reincarnation", "Snake Sage is a major visual/mechanical transformation and ideal one-step Awakening."),

    P("might-guy-base", "might-guy", "Might Guy", "Base", "Eighth Gate", "Eight Gates Released Formation", 93, 98, "BURST_DPS", "SHIPPUDEN", "EIGHT_GATES", "MANGA_CANON", EIGHT_GATES,
      "Strong Fist; Dynamic Entry; Morning Peacock; counters; gate-stacking passive", "Night Guy", "Lower Gates remain temporary skill/passive states; the Eighth Gate is reserved for the one spectacular Awakening."),
    P("rock-lee-genin", "rock-lee", "Rock Lee [Genin]", "Genin", "Fifth Gate", "Fifth Gate Lee", 84, 91, "BURST_DPS", "PART_I", "EIGHT_GATES", "MANGA_CANON", EIGHT_GATES,
      "Strong Fist; weights release; Leaf Hurricane; Primary Lotus; gate buildup", "Reverse Lotus", "Part I Lee has an iconic Fifth Gate climax."),
    P("rock-lee-shippuden", "rock-lee", "Rock Lee [Shippuden]", "Shippuden", "Sixth Gate", "Sixth Gate Lee", 82, 88, "BURST_DPS", "SHIPPUDEN", "EIGHT_GATES", "MANGA_CANON", EIGHT_GATES,
      "advanced Strong Fist; high-speed chains; defensive footwork; Lotus variants; gate buildup", "Sixth Gate shockwave combo", "The later Lee version reserves the strongest census Gate available without creating every Gate as a hero."),

    P("choji-shippuden", "choji-akimichi", "Choji [Shippuden]", "Shippuden", "Butterfly Mode", "Butterfly Choji", 85, 94, "BRUISER", "SHIPPUDEN", "CLAN_MODE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Ch%C5%8Dji_Akimichi",
      "Partial Expansion; Human Boulder; ally protection; calorie control; size scaling", "Butterfly Bombing", "Butterfly Mode is the canonical visible Akimichi power spike and natural Awakening."),

    P("sasori-hiruko", "sasori", "Sasori [Hiruko]", "Hiruko Puppet", "True Body", "Sasori True Body", 90, 96, "SUMMONER_CONTROL", "AKATSUKI", "PUPPET_BODY", "MANGA_CANON", "https://naruto.fandom.com/wiki/Sasori",
      "Hiruko shell; poison needles; puppet defense; traps; poison passive", "Third Kazekage puppet assault", "Breaking out of Hiruko to reveal Sasori's puppet body is an unmistakable visual transformation; Hundred Puppets remains an Ultimate."),
    P("kisame-akatsuki", "kisame-hoshigaki", "Kisame [Akatsuki]", "Akatsuki", "Samehada Fusion", "Samehada Fusion", 92, 95, "TANK_DPS", "AKATSUKI", "FUSION", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kisame_Hoshigaki",
      "Samehada drain; water prison; shark projectiles; chakra theft; sustain", "Water Prison Shark Dance", "Fusion changes body silhouette, mobility, sustain, and water control while preserving Kisame's base identity."),
    P("kakuzu-akatsuki", "kakuzu", "Kakuzu [Akatsuki]", "Akatsuki", "Earth Grudge Fear", "Earth Grudge Fear Released", 89, 90, "TANK_DPS", "AKATSUKI", "BODY_TRANSFORMATION", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kakuzu",
      "thread repair; elemental masks; durability; bounty pressure; multi-heart passive", "four-mask elemental barrage", "Earth Grudge Fear visibly exposes the thread/mask combat state and supports a unique sixth skill."),
    P("konan-akatsuki", "konan", "Konan [Akatsuki]", "Akatsuki", "Paper Angel", "Paper Angel Konan", 86, 93, "CONTROL_DPS", "AKATSUKI", "PAPER_FORM", "MANGA_CANON", "https://naruto.fandom.com/wiki/Konan",
      "paper shuriken; clone paper; flight; bind; preparation passive", "Paper Person of God", "Paper Angel is visually dramatic and preserves Konan's paper identity while enabling a signature Awakening skill."),
    P("danzo-candidate", "danzo-shimura", "Danzo [Hokage Candidate]", "Hokage Candidate", "Izanagi Arm", "Izanagi Arm Unsealed", 85, 89, "CONTROL_TANK", "FIVE_KAGE_SUMMIT", "DOJUTSU_SEAL", "MANGA_CANON", "https://naruto.fandom.com/wiki/Danz%C5%8D_Shimura",
      "Wind Release; Baku summon; sealing; Sharingan resource; ruthless sacrifice passive", "Izanagi cycle", "Unsealing the Sharingan arm is an immediate visual/gameplay power-state change suitable for the one Awakening."),
    P("shisui-anbu", "shisui-uchiha", "Shisui [ANBU]", "ANBU", "Mangekyo Sharingan", "Mangekyo Shisui", 86, 91, "CONTROL_ASSASSIN", "ANBU", "DOJUTSU", "MANGA_CANON", "https://naruto.fandom.com/wiki/Shisui_Uchiha",
      "Body Flicker; fire style; shuriken; genjutsu; evasion", "Kotoamatsukami", "Mangekyo is the reserved ocular escalation and gives a unique control-focused sixth skill."),

    P("toneri-base", "toneri-otsutsuki", "Toneri Otsutsuki", "Base", "Tenseigan Chakra Mode", "Tenseigan Chakra Mode", 90, 96, "CONTROL_DPS", "THE_LAST", "TENSEIGAN", "OFFICIAL_MOVIE", "https://naruto.fandom.com/wiki/Toneri_%C5%8Ctsutsuki",
      "puppet control; chakra spheres; attraction/repulsion; lunar constructs; Tenseigan setup", "Golden Wheel Reincarnation Explosion", "The Tenseigan Chakra Mode is a canonical movie transformation with major visual and mechanical change."),
    P("kaguya-base", "kaguya-otsutsuki", "Kaguya Otsutsuki", "Base", "Rabbit Goddess", "Rabbit Goddess", 97, 90, "BOSS_CONTROL", "ANCIENT", "BODY_TRANSFORMATION", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kaguya_%C5%8Ctsutsuki",
      "dimension swap; ash bones; chakra absorption; Byakugan; gravity-zone control", "Expansive Truth-Seeking Ball", "The rabbit-goddess state is a visually distinct high-power body form and remains exclusive to Kaguya's Awakening."),
    P("hagoromo-young", "hagoromo-otsutsuki", "Hagoromo [Young]", "Young", "Sage of Six Paths", "Sage of Six Paths", 92, 94, "UTILITY_CONTROL", "ANCIENT", "SIX_PATHS", "ANIME_CANON", "https://naruto.fandom.com/wiki/Hagoromo_%C5%8Ctsutsuki",
      "ninshu support; chakra constructs; sealing; elemental utility; ocular growth", "Six Paths Chibaku Tensei", "The Sage of Six Paths identity is a clear later state with iconic visual/ability escalation."),
    P("asura-base", "asura-otsutsuki", "Asura Otsutsuki", "Base", "Six Paths Avatar", "Six Paths Avatar", 84, 89, "BRUISER_SUPPORT", "ANCIENT", "CHAKRA_AVATAR", "ANIME_CANON", "https://naruto.fandom.com/wiki/Asura_%C5%8Ctsutsuki",
      "cooperative ninshu; stamina; chakra constructs; ally buffs; endurance", "Six Paths avatar barrage", "The chakra avatar is the recognizable high-level escalation represented in the census."),
    P("indra-base", "indra-otsutsuki", "Indra Otsutsuki", "Base", "Susanoo", "Susanoo Indra", 84, 90, "CONTROL_DPS", "ANCIENT", "SUSANOO", "ANIME_CANON", "https://naruto.fandom.com/wiki/Indra_%C5%8Ctsutsuki",
      "Sharingan; lightning; fire style; precision; solitary-power passive", "Susanoo arrow", "Susanoo is the strongest visible census transformation and preserves the one-Awakening rule."),

    P("kimimaro-base", "kimimaro", "Kimimaro", "Base", "Curse Mark Level 2", "Curse Mark Level 2", 88, 94, "BRUISER_ASSASSIN", "PART_I", "CURSED_SEAL", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kimimaro",
      "bone blades; dance forms; armor bones; regeneration; curse-mark buildup", "Bracken Dance eruption", "Curse Mark Level 2 is a canonical full visual transformation and obvious one-step Awakening."),
    P("jugo-base", "jugo", "Jugo", "Curse Mark Base", "Sage Transformation", "Sage Transformation", 86, 93, "BRUISER", "SHIPPUDEN", "SAGE_TRANSFORMATION", "MANGA_CANON", "https://naruto.fandom.com/wiki/J%C5%ABgo",
      "natural-energy absorption; body weapons; rage; ally chakra transfer; adaptive limbs", "full Sage Transformation cannon", "Full Sage Transformation is the canonical visible escalation of Jugo's clan ability."),

    P("tayuya-base", "tayuya", "Tayuya", "Sound Four", "Curse Mark Level 2", "Curse Mark Level 2", 78, 88, "CONTROL", "PART_I", "CURSED_SEAL", "MANGA_CANON", CURSED_SEAL,
      "flute genjutsu; Doki summons; sound pressure; bind; curse buildup", "Doki demon chorus", "Curse Mark Level 2 is a canonical visible escalation."),
    P("kidomaru-base", "kidomaru", "Kidomaru", "Sound Four", "Curse Mark Level 2", "Curse Mark Level 2", 78, 88, "RANGED_DPS", "PART_I", "CURSED_SEAL", "MANGA_CANON", CURSED_SEAL,
      "web traps; spider summon; bow sniping; armor; curse buildup", "war bow snipe", "Curse Mark Level 2 is a canonical visible escalation."),
    P("sakon-base", "sakon", "Sakon", "Sound Four", "Curse Mark Level 2", "Curse Mark Level 2", 77, 87, "ASSASSIN", "PART_I", "CURSED_SEAL", "MANGA_CANON", CURSED_SEAL,
      "body invasion; twin attacks; close combat; debuff; curse buildup", "parasitic body split", "Curse Mark Level 2 is a canonical visible escalation."),
    P("ukon-base", "ukon", "Ukon", "Sound Four", "Curse Mark Level 2", "Curse Mark Level 2", 74, 84, "ASSASSIN", "PART_I", "CURSED_SEAL", "MANGA_CANON", CURSED_SEAL,
      "body merge; ambush; twin assist; debuff; curse buildup", "cellular invasion", "Curse Mark Level 2 is a canonical visible escalation."),
    P("jirobo-base", "jirobo", "Jirobo", "Sound Four", "Curse Mark Level 2", "Curse Mark Level 2", 77, 88, "TANK", "PART_I", "CURSED_SEAL", "MANGA_CANON", CURSED_SEAL,
      "earth walls; chakra drain; strength; prison; curse buildup", "curse-mark power slam", "Curse Mark Level 2 is a canonical visible escalation."),

    P("fourth-raikage-base", "a-fourth-raikage", "Fourth Raikage", "Raikage", "Lightning Armor", "Lightning Release Chakra Mode", 91, 92, "BRUISER_ASSASSIN", "FIVE_KAGE_SUMMIT", "CHAKRA_MODE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Lightning_Release_Chakra_Mode",
      "lariat; lightning speed; grapples; armor; rage pressure", "Guillotine Drop overdrive", "Lightning Release Chakra Mode visibly changes aura/speed and is the core high-power state."),
    P("might-duy-base", "might-duy", "Might Duy", "Eternal Genin", "Eight Gates", "Eight Gates Released Formation", 74, 90, "BURST_DPS", "PRE_SERIES", "EIGHT_GATES", "MANGA_CANON", EIGHT_GATES,
      "Strong Fist; perseverance; gate release; rescue pressure; self-sacrifice", "Eight Gates final stand", "His defining canonical peak is the Eight Gates, making it the natural single Awakening."),
    P("kinkaku-base", "kinkaku", "Kinkaku", "Gold Brother", "Nine-Tails Cloak", "Nine-Tails Cloak", 82, 90, "BRUISER", "WAR_ERA", "JINCHURIKI_LIKE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Kinkaku",
      "Treasured Tools; chakra rope; word curse; durability; Nine-Tails chakra passive", "Nine-Tails cloak rampage", "The cloak is a canonical visible power state and should not be another summonable Kinkaku."),
    P("ginkaku-base", "ginkaku", "Ginkaku", "Silver Brother", "Nine-Tails Cloak", "Nine-Tails Cloak", 82, 90, "CONTROL", "WAR_ERA", "JINCHURIKI_LIKE", "MANGA_CANON", "https://naruto.fandom.com/wiki/Ginkaku",
      "Treasured Tools; sealing gourd; word curse; combo support; Nine-Tails chakra passive", "Nine-Tails cloak seal rush", "The cloak is a canonical visible power state and should remain exclusive to Awakening."),

    P("sora-base", "sora", "Sora", "Temple Monk", "Pseudo Jinchuriki", "Pseudo Jinchuriki Sora", 72, 85, "BRUISER", "ANIME_ORIGINAL", "JINCHURIKI_LIKE", "ANIME_ORIGINAL", "https://naruto.fandom.com/wiki/Sora",
      "wind claw; monk taijutsu; chakra pressure; anger passive; sealing interaction", "Nine-Tails chakra arm", "Anime-original but canonical-to-its-source pseudo-jinchuriki transformation is a visible one-step Awakening."),
    P("menma-base", "menma-uzumaki", "Menma Uzumaki", "Masked Menma", "Nine-Tails Menma", "Nine-Tails Menma", 78, 90, "CONTROL_DPS", "MOVIE_ORIGINAL", "JINCHURIKI", "OFFICIAL_MOVIE", "https://naruto.fandom.com/wiki/Menma_Uzumaki",
      "black Rasengan; masked summons; genjutsu pressure; mobility; dark chakra", "Black Nine-Tails assault", "Movie-original Menma's Nine-Tails state is the obvious visual awakening for the same collectible identity."),
    P("mukade-base", "mukade", "Mukade", "Suna Missing Nin", "Ryumyaku Empowered", "Ryumyaku Empowered Mukade", 70, 82, "SUMMONER_CONTROL", "MOVIE_ORIGINAL", "ENERGY_TRANSFORMATION", "OFFICIAL_MOVIE", "https://naruto.fandom.com/wiki/Mukade",
      "puppet network; chakra threads; absorption; terrain control; Ryumyaku setup", "Ryumyaku puppet body", "The movie explicitly empowers and transforms Mukade through the Ryumyaku; it is kept as one Awakening rather than a second hero."),
    P("shinno-base", "shinno", "Shinno", "Doctor", "Eight Gates Body", "Eight Gates Body", 72, 84, "BRUISER", "MOVIE_ORIGINAL", "BODY_TRANSFORMATION", "OFFICIAL_MOVIE", "https://naruto.fandom.com/wiki/Shinn%C5%8D",
      "medical deception; body control; dark chakra; drain; regeneration", "Body Revival overdrive", "The movie transformation is a distinct visual combat body and suitable one-step Awakening."),
]

# Extra researched forms not present in the legacy census but needed to preserve one-Awakening pair quality.
EXTRA_FORMS = [
    ("naruto-uzumaki", "Asura Kurama Mode", "WAR_ARC", 10, "MANGA_CANON", NARUTO, "Reserved exclusively for naruto-six-paths Awakening."),
    ("naruto-uzumaki", "Baryon Mode", "ADULT", 10, "MANGA_CANON", NARUTO, "Reserved exclusively for naruto-hokage Awakening."),
]

# Explicit classifications for variants that should not be inferred from their label.
FORCE_CLASS: dict[tuple[str, str], tuple[str, str, str]] = {
    ("naruto-uzumaki", "Academy"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Early appearance; insufficient standalone combat identity."),
    ("naruto-uzumaki", "Four-Tails Cloak"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Berserk temporary state; not a second persistent Awakening."),
    ("naruto-uzumaki", "KCM1"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Intermediate Kurama state reserved inside Sage-era progression/skill visuals."),
    ("naruto-uzumaki", "Bijuu Mode"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Large avatar manifestation is better used by Ultimate/Awakening skill presentation."),
    ("sasuke-uchiha", "Six Paths"): ("MERGED_OR_REMOVED_DUPLICATE", "MERGE", "Six Paths power is represented by the Rinnegan hero/Awakening identity, not a duplicate summonable form."),
    ("kakashi-hatake", "Sharingan"): ("AWAKENING_FORM", "MOVE_TO_AWAKENING", "Reserved for Young Kakashi Awakening; main-series Kakashi uses Mangekyo base boundary."),
    ("itachi-uchiha", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation is not a normal Awakening and does not need a duplicate hero here."),
    ("madara-uchiha", "Warring States"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance around the EMS identity."),
    ("madara-uchiha", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Edo is not an Awakening; Rinnegan combat identity is represented directly."),
    ("madara-uchiha", "Six Paths"): ("MERGED_OR_REMOVED_DUPLICATE", "MERGE", "Ten-Tails jinchuriki row already represents the persistent Six Paths transformation."),
    ("obito-uchiha", "Tobi"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Alias/costume presentation of the masked identity, not a separate kit."),
    ("minato-namikaze", "Fourth Hokage"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Rank/title does not by itself create a new combat form."),
    ("hashirama-senju", "Warring States"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance of the same wood-style identity."),
    ("hashirama-senju", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation is not used as Awakening."),
    ("tobirama-senju", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation skin/state."),
    ("hiruzen-sarutobi", "Third Hokage"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Title/era label; Prime hero carries the full combat identity."),
    ("hiruzen-sarutobi", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation skin/state."),
    ("gaara", "War Arc"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era presentation of Kazekage Gaara."),
    ("killer-b", "Full Gyuki"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Full tailed-beast manifestation is Ultimate-scale, not another persistent hero."),
    ("nagato", "Young"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Backstory appearance; insufficient separate five-skill identity in current design."),
    ("nagato", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation is not a normal Awakening."),
    ("orochimaru", "Akatsuki Era"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era/costume variation."),
    ("orochimaru", "Shippuden"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era label of Sannin identity."),
    ("kabuto-yakushi", "War Arc"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Snake Sage is the actual transformation; War Arc is an era label."),
    ("might-guy", "Sixth Gate"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Lower Gate reserved for skill/passive state."),
    ("might-guy", "Seventh Gate"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Lower Gate reserved for skill/passive state."),
    ("neji-hyuga", "Genin"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Same core Gentle Fist identity; Shippuden retained as final hero boundary."),
    ("hinata-hyuga", "Genin"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance; Shippuden is the retained hero boundary."),
    ("hinata-hyuga", "War Arc"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance of the same Shippuden combat identity."),
    ("shikamaru-nara", "Genin"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Age/rank alone does not justify duplicate hero."),
    ("shikamaru-nara", "Chunin"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Rank change alone is not a transformation."),
    ("shikamaru-nara", "War Arc"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance; Shippuden hero retains the kit."),
    ("choji-akimichi", "Genin"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Age/era appearance; Shippuden is the hero boundary."),
    ("choji-akimichi", "War Arc"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance."),
    ("ino-yamanaka", "Genin"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Age/era appearance; Shippuden is the hero boundary."),
    ("ino-yamanaka", "War Arc"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Era appearance."),
    ("deidara", "C2 Dragon"): ("SKILL_OR_ULTIMATE", "MOVE_TO_SKILL", "Named technique/construct, not a persistent hero form."),
    ("deidara", "C4"): ("SKILL_OR_ULTIMATE", "MOVE_TO_ULTIMATE", "Named ultimate-scale technique, not a hero."),
    ("deidara", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation skin/state."),
    ("sasori", "Hundred Puppets"): ("SKILL_OR_ULTIMATE", "MOVE_TO_ULTIMATE", "Technique/army manifestation rather than persistent form."),
    ("sasori", "Edo Tensei"): ("COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation skin/state."),
    ("hidan", "Curse Ritual"): ("SKILL_OR_ULTIMATE", "MOVE_TO_ULTIMATE", "Ritual technique, not a separate hero."),
    ("chiyo", "Ten Puppets"): ("SKILL_OR_ULTIMATE", "MOVE_TO_ULTIMATE", "Secret White Move puppet technique."),
    ("hamura-otsutsuki", "Tenseigan Legacy"): ("MERGED_OR_REMOVED_DUPLICATE", "MERGE", "Legacy/title wording is not a documented persistent Hamura transformation."),
    ("rin-nohara", "Three-Tails Vessel"): ("TEMPORARY_COMBAT_FORM", "MOVE_TO_TEMP_FORM", "Rin was a vessel; no controlled persistent combat transformation is fabricated."),
    ("kiba-inuzuka", "Two-Headed Wolf"): ("COOPERATION_FORM_OR_TECHNIQUE", "MOVE_TO_COOP", "Fusion/cooperation technique with Akamaru, not a solo persistent hero form."),
    ("fukasaku", "Genjutsu Chorus"): ("COOPERATION_FORM_OR_TECHNIQUE", "MOVE_TO_COOP", "Cooperation technique with Shima."),
    ("shima", "Genjutsu Chorus"): ("COOPERATION_FORM_OR_TECHNIQUE", "MOVE_TO_COOP", "Cooperation technique with Fukasaku."),
    ("ajisai", "Animal Path Vessel"): ("SPECIAL_INDEPENDENT_CHARACTER", "MERGE", "Corpse/vessel identity for Pain content, not Ajisai's normal Awakening."),
}

# Named attacks, weapons-in-action, summons and combat techniques are not persistent forms.
TECHNIQUE_HINTS = (
    "c2 dragon", "c4", "puppets", "ritual", "two-headed wolf", "nano insects", "mind transfer",
    "crescent moon blade", "particle style", "heavenly transfer", "mountain jutsu", "shadow clone",
    "resonating echo", "bell genjutsu", "air pressure arms", "chakra absorption", "spore clone",
    "genjutsu chorus", "adamantine staff", "hundred healings support", "orochimaru summon",
    "sasuke summon", "gamabunta battle", "sage training", "chain gauntlet", "elemental kiss",
    "umbrella needles", "senbon assault", "umbrella barrage", "clone ambush", "earth ambush", "mist ambush",
    "lightning burial", "water style veteran", "wind blade", "stone golem", "camouflage",
)

ERA_OR_TITLE_HINTS = (
    "war arc", "team captain", "team 7", "team 8 leader", "guard", "leader", "instructor", "proctor",
    "chief", "attendant", "medical corps", "clan head", "police captain", "heiress", "gate guard",
    "special jonin", "jonin", "chunin", "academy", "prime", "disciple", "sannin attendant", "root spy",
    "raikage secretary", "third raikage aide", "kazekage guard", "sand chunin", "cloud jonin", "cloud ninja",
    "sand elder", "sand jonin", "sand medic", "sand kunoichi", "sensor captain", "raikage guard",
    "tsuchikage guard", "earth commander", "allied strategy chief", "allied intelligence chief",
    "intelligence corps", "interrogation chief", "fourth hokage guard", "root operative", "akatsuki founder",
    "amegakure leader", "kabuto squad", "samurai captain", "war fan master", "master puppeteer",
)

TRANSFORM_HINTS = (
    "cloak", "mode", "transformation", "fusion", "curse mark level 2", "mangekyo sharingan",
    "mangekyo", "rabbit goddess", "susanoo", "paper angel", "earth grudge fear", "izanagi arm",
    "cursed power", "pseudo jinchuriki", "nine-tails menma", "ryumyaku empowered", "eight gates body",
)


def census_rows() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    seen: set[tuple[str, str]] = set()
    for path in sorted(REFERENCE.glob("variant-census*.csv")):
        for row in read_csv(path):
            key = (row["character_id"].strip(), row["variant"].strip())
            if key in seen:
                continue
            seen.add(key)
            rows.append({"character_id": key[0], "variant": key[1], "source_status": row.get("status", "")})
    return rows


def roster_groups() -> dict[str, str]:
    return {r["id"].strip(): r["group"].strip() for r in read_csv(REFERENCE / "roster-complete.csv")}


def kit_profiles() -> dict[str, str]:
    return {r["character_id"].strip(): r["profile_id"].strip() for r in read_csv(SKILLS / "character-kit-map.csv")}


def pair_maps(pairs: list[Pair]):
    base: dict[tuple[str, str], Pair] = {}
    awake: dict[tuple[str, str], Pair] = {}
    for p in pairs:
        key = (p.character_id, p.base_variant)
        if key in base:
            raise ValueError(f"duplicate base form pair {key}")
        base[key] = p
        if p.awakening_variant:
            akey = (p.character_id, p.awakening_variant)
            if akey in awake:
                raise ValueError(f"awakening form reused by multiple heroes: {akey}")
            awake[akey] = p
    return base, awake


def classify_unpaired(character_id: str, variant: str, index: int, group: str) -> tuple[str, str, str]:
    low = variant.lower()
    if group == "Summons":
        if index == 0:
            return "SPECIAL_INDEPENDENT_CHARACTER", "KEEP_AS_HERO", "Summon creature is retained as special independent combat content, not a normal ninja hero version."
        return "SKILL_OR_ULTIMATE", "MOVE_TO_SKILL", "Summon-specific action/state belongs to technique presentation rather than another summonable unit."
    if "edo tensei" in low or "reanimated" in low:
        return "COSMETIC_SKIN", "MOVE_TO_SKIN", "Reanimation is explicitly not treated as a normal Awakening."
    if any(h in low for h in TECHNIQUE_HINTS):
        action = "MOVE_TO_ULTIMATE" if any(k in low for k in ("c4", "hundred puppets", "ten puppets", "ritual")) else "MOVE_TO_SKILL"
        return "SKILL_OR_ULTIMATE", action, "Label describes a technique/weapon action rather than a persistent hero form."
    if any(h in low for h in ERA_OR_TITLE_HINTS) and index > 0:
        return "COSMETIC_SKIN", "MOVE_TO_SKIN", "Era/rank/title difference alone does not justify another collectible hero."
    if index == 0:
        return "COLLECTIBLE_HERO_VERSION", "KEEP_AS_HERO", "Primary source variant retained as the character's baseline collectible version pending deeper character-specific research."
    if any(h in low for h in TRANSFORM_HINTS):
        return "AWAKENING_FORM", "MOVE_TO_AWAKENING", "Visibly transformed combat state reserved as the one Awakening for the baseline hero."
    return "MERGED_OR_REMOVED_DUPLICATE", "MERGE", "Secondary label does not currently clear the standalone-hero threshold and is merged into the character's primary kit/presentation."


def auto_pairs(rows: list[dict[str, str]], explicit_pairs: list[Pair], groups: dict[str, str], kits: dict[str, str]) -> list[Pair]:
    base_map, awake_map = pair_maps(explicit_pairs)
    by_char: dict[str, list[str]] = defaultdict(list)
    for row in rows:
        by_char[row["character_id"]].append(row["variant"])
    chars_with_pair = {p.character_id for p in explicit_pairs}
    out: list[Pair] = list(explicit_pairs)
    for character_id, variants in sorted(by_char.items()):
        if character_id in chars_with_pair or groups.get(character_id) == "Summons":
            continue
        # First non-technique source row becomes the single baseline hero.
        base_variant = variants[0]
        awakening_variant = None
        for v in variants[1:]:
            cls, _, _ = classify_unpaired(character_id, v, variants.index(v), groups.get(character_id, ""))
            if cls == "AWAKENING_FORM":
                awakening_variant = v
                break
        kit = kits.get(character_id, "character-specific")
        out.append(P(
            f"{character_id}-core", character_id, f"{character_id} [Core]", base_variant,
            awakening_variant, awakening_variant or "", 68 if awakening_variant is None else 72,
            76 if awakening_variant else None, "FLEX", "SOURCE_ERA", "CANON_FORM" if awakening_variant else "NONE",
            "DESIGN_INTERPRETATION", f"current variant census; current kit seed={kit}",
            f"rebuild five explicit slots from the {kit} seed without generic fallback",
            f"{awakening_variant} signature technique" if awakening_variant else "",
            "Minor/support character keeps one differentiated collectible identity; a transformation is only reserved when the census label is visibly form-like."
        ))
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args()

    rows = census_rows()
    groups = roster_groups()
    kits = kit_profiles()
    pairs = auto_pairs(rows, PAIRS, groups, kits)
    base_map, awake_map = pair_maps(pairs)

    by_char_rows: dict[str, list[dict[str, str]]] = defaultdict(list)
    for row in rows:
        by_char_rows[row["character_id"]].append(row)

    migration_rows: list[dict[str, object]] = []
    form_rows: list[dict[str, object]] = []
    form_role: dict[tuple[str, str], str] = {}

    for character_id, variants in by_char_rows.items():
        group = groups.get(character_id, "")
        for index, row in enumerate(variants):
            variant = row["variant"]
            key = (character_id, variant)
            if key in base_map:
                p = base_map[key]
                classification, action, reason = "COLLECTIBLE_HERO_VERSION", "KEEP_AS_HERO", f"Base form of proposed hero {p.hero_id}."
            elif key in awake_map:
                p = awake_map[key]
                classification, action, reason = "AWAKENING_FORM", "MOVE_TO_AWAKENING", f"Reserved exclusively as Awakening of {p.hero_id}."
            elif key in FORCE_CLASS:
                classification, action, reason = FORCE_CLASS[key]
            else:
                classification, action, reason = classify_unpaired(character_id, variant, index, group)

            if classification not in CLASSIFICATIONS:
                raise ValueError(f"invalid classification {classification}")
            form_role[key] = classification

            hero_id = base_map[key].hero_id if key in base_map else ""
            awakening_id = f"awakening-{awake_map[key].hero_id}" if key in awake_map else ""
            new_skill_id = f"legacy-{character_id}-{slug(variant)}" if classification == "SKILL_OR_ULTIMATE" else ""
            new_skin_id = f"skin-{character_id}-{slug(variant)}" if classification == "COSMETIC_SKIN" else ""
            temp_id = f"temp-{character_id}-{slug(variant)}" if classification == "TEMPORARY_COMBAT_FORM" else ""
            confidence = "HIGH" if key in base_map or key in awake_map or key in FORCE_CLASS else "MEDIUM"
            migration_rows.append({
                "character_id": character_id,
                "old_variant": variant,
                "old_variant_id": form_id(character_id, variant),
                "classification": classification,
                "new_hero_id": hero_id,
                "new_awakening_form_id": awakening_id,
                "new_skill_id": new_skill_id,
                "new_skin_id": new_skin_id,
                "temporary_state_id": temp_id,
                "migration_action": action,
                "reason": reason,
                "confidence": confidence,
            })

            is_hero = classification == "COLLECTIBLE_HERO_VERSION"
            is_awake = classification == "AWAKENING_FORM"
            is_temp = classification == "TEMPORARY_COMBAT_FORM"
            is_skill = classification == "SKILL_OR_ULTIMATE"
            is_coop = classification == "COOPERATION_FORM_OR_TECHNIQUE"
            p = base_map.get(key) or awake_map.get(key)
            hero_score = p.hero_score if is_hero and p else (68 if is_hero else 0)
            awakening_score = p.pair_score if is_awake and p and p.pair_score else (76 if is_awake else 0)
            canon_type = p.canon_confidence if p else ("DESIGN_INTERPRETATION" if classification in {"MERGED_OR_REMOVED_DUPLICATE", "COSMETIC_SKIN"} else "SOURCE_RESEARCH_REQUIRED")
            canon_source = p.source_reference if p else "current variant census; character-specific canon source required before production READY"
            form_rows.append({
                "character_id": character_id,
                "form_id": form_id(character_id, variant),
                "form_name": variant,
                "era": p.era if p else "SOURCE_ERA",
                "power_level": 10 if is_awake else 5 if is_hero else 0,
                "visual_difference": 10 if is_awake else 6 if is_hero else 3,
                "combat_difference": 10 if is_awake else 6 if is_hero else 3,
                "is_persistent_form": str(is_hero or is_awake).lower(),
                "is_temporary_form": str(is_temp).lower(),
                "is_technique": str(is_skill).lower(),
                "is_coop": str(is_coop).lower(),
                "standalone_hero_score": hero_score,
                "awakening_score": awakening_score,
                "canon_type": canon_type,
                "canon_source": canon_source,
                "notes": reason,
            })

    # Add researched forms that were intentionally missing from the legacy variant census.
    for character_id, variant, era, power, canon_type, source, note in EXTRA_FORMS:
        key = (character_id, variant)
        if any(r["character_id"] == character_id and r["form_name"] == variant for r in form_rows):
            continue
        p = next((p for p in pairs if p.character_id == character_id and p.awakening_variant == variant), None)
        form_rows.append({
            "character_id": character_id,
            "form_id": form_id(character_id, variant),
            "form_name": variant,
            "era": era,
            "power_level": power,
            "visual_difference": 10,
            "combat_difference": 10,
            "is_persistent_form": "true",
            "is_temporary_form": "false",
            "is_technique": "false",
            "is_coop": "false",
            "standalone_hero_score": 0,
            "awakening_score": p.pair_score if p and p.pair_score else 90,
            "canon_type": canon_type,
            "canon_source": source,
            "notes": note,
        })
        form_role[key] = "AWAKENING_FORM"

    pair_rows: list[dict[str, object]] = []
    for p in sorted(pairs, key=lambda x: (x.character_id, x.hero_id)):
        pair_rows.append({
            "hero_id": p.hero_id,
            "character_id": p.character_id,
            "hero_version_name": p.hero_version_name,
            "base_form_id": form_id(p.character_id, p.base_variant),
            "awakening_form_id": form_id(p.character_id, p.awakening_variant) if p.awakening_variant else "",
            "awakening_name": p.awakening_name,
            "hero_version_score": p.hero_score,
            "pair_score": p.pair_score or "",
            "role": p.role,
            "era": p.era,
            "awakening_type": p.awakening_type,
            "canon_confidence": p.canon_confidence,
            "source_reference": p.source_reference,
            "design_reason": p.reason,
            "status": "PROPOSED_RESEARCH_BASELINE" if p.canon_confidence != "DESIGN_INTERPRETATION" else "PROPOSED_NEEDS_SOURCE_REVIEW",
        })

    # Structural validation required before production migration.
    errors: list[str] = []
    source_keys = {(r["character_id"], r["variant"]) for r in rows}
    migration_keys = {(r["character_id"], r["old_variant"]) for r in migration_rows}
    if source_keys != migration_keys:
        errors.append(f"variant migration coverage mismatch source={len(source_keys)} migration={len(migration_keys)}")
    if len(source_keys) != 427:
        errors.append(f"expected current census=427, got {len(source_keys)}")

    hero_ids = [p.hero_id for p in pairs]
    if len(hero_ids) != len(set(hero_ids)):
        errors.append("hero_id must be unique")
    awakening_forms = [(p.character_id, p.awakening_variant) for p in pairs if p.awakening_variant]
    if len(awakening_forms) != len(set(awakening_forms)):
        errors.append("awakening form reused by more than one hero")
    base_forms = {(p.character_id, p.base_variant) for p in pairs}
    awake_forms = {(p.character_id, p.awakening_variant) for p in pairs if p.awakening_variant}
    overlap = base_forms & awake_forms
    if overlap:
        errors.append(f"form cannot be both base hero and awakening: {sorted(overlap)[:5]}")
    for p in pairs:
        if p.hero_score < 65:
            errors.append(f"hero score below 65: {p.hero_id}={p.hero_score}")
        if p.awakening_variant and (p.pair_score is None or p.pair_score < 70):
            errors.append(f"pair score below 70: {p.hero_id}={p.pair_score}")
    if errors:
        for e in errors:
            print("ERROR", e)
        return 1

    if args.write:
        write_csv(DESIGN / "character-form-pool.csv", [
            "character_id", "form_id", "form_name", "era", "power_level", "visual_difference",
            "combat_difference", "is_persistent_form", "is_temporary_form", "is_technique", "is_coop",
            "standalone_hero_score", "awakening_score", "canon_type", "canon_source", "notes"
        ], sorted(form_rows, key=lambda r: (str(r["character_id"]), str(r["form_name"]))))
        write_csv(DESIGN / "hero-awakening-pairs.csv", [
            "hero_id", "character_id", "hero_version_name", "base_form_id", "awakening_form_id",
            "awakening_name", "hero_version_score", "pair_score", "role", "era", "awakening_type",
            "canon_confidence", "source_reference", "design_reason", "status"
        ], pair_rows)
        write_csv(DESIGN / "variant-reclassification.csv", [
            "character_id", "old_variant", "old_variant_id", "classification", "new_hero_id",
            "new_awakening_form_id", "new_skill_id", "new_skin_id", "temporary_state_id",
            "migration_action", "reason", "confidence"
        ], sorted(migration_rows, key=lambda r: (str(r["character_id"]), str(r["old_variant"]))))

        DOCS.mkdir(parents=True, exist_ok=True)
        lines = [
            "# Hero / Awakening Pair Proposal",
            "",
            "> This is the mandatory pre-migration design output. It treats the legacy variant census as source material, not the final roster.",
            "",
            f"- Source variants classified: **{len(source_keys)} / 427**",
            f"- Proposed collectible Hero Versions: **{len(pairs)}**",
            f"- Proposed Hero→Awakening pairs: **{sum(1 for p in pairs if p.awakening_variant)}**",
            f"- Proposed heroes without Awakening: **{sum(1 for p in pairs if not p.awakening_variant)}**",
            "- Structural rule: every Hero Version has exactly 0..1 Awakening; no Awakening form is another Hero Version.",
            "- Multi-stage legacy evolution files are **not** accepted as the final Awakening model.",
            "",
            "## Research and confidence policy",
            "",
            "Major pairs use explicit canon-reference links and confidence labels. Generic/minor rows remain DESIGN_INTERPRETATION or SOURCE_RESEARCH_REQUIRED until their character-specific canon reference is reviewed; they are classified now so the full 427-row census has no unmapped variant.",
            "",
        ]
        migrations_by_char: dict[str, list[dict[str, object]]] = defaultdict(list)
        for row in migration_rows:
            migrations_by_char[str(row["character_id"])].append(row)
        pairs_by_char: dict[str, list[Pair]] = defaultdict(list)
        for p in pairs:
            pairs_by_char[p.character_id].append(p)

        for character_id in sorted(migrations_by_char):
            lines += [f"## {character_id}", ""]
            for p in sorted(pairs_by_char.get(character_id, []), key=lambda x: x.hero_id):
                lines += [
                    f"### {p.hero_version_name}",
                    f"- **Hero ID:** `{p.hero_id}`",
                    f"- **Base form:** {p.base_variant}",
                    f"- **Awakening:** {p.awakening_variant or 'None — no canonical/valuable stronger persistent form reserved'}",
                    f"- **5-skill identity:** {p.identity}",
                    f"- **Awakening Skill concept:** {p.awakening_skill_concept or 'N/A'}",
                    f"- **Hero version score:** {p.hero_score}/100",
                    f"- **Pair score:** {str(p.pair_score) + '/100' if p.pair_score is not None else 'N/A'}",
                    f"- **Canon confidence:** {p.canon_confidence}",
                    f"- **Source:** {p.source_reference}",
                    f"- **Reason:** {p.reason}",
                    "",
                ]
            unused = [r for r in migrations_by_char[character_id] if r["classification"] not in {"COLLECTIBLE_HERO_VERSION", "AWAKENING_FORM"}]
            if unused:
                lines += ["### Reclassified / unused source forms", ""]
                for r in sorted(unused, key=lambda x: str(x["old_variant"])):
                    lines.append(f"- **{r['old_variant']}** → `{r['classification']}` / `{r['migration_action']}` — {r['reason']}")
                lines.append("")

        lines += [
            "## Structural validation result",
            "",
            "- 427/427 source variants mapped.",
            "- Hero IDs unique.",
            "- Awakening forms unique.",
            "- No form is both collectible Hero Version and Awakening.",
            "- Every proposed collectible hero score is >= 65.",
            "- Every proposed Hero→Awakening pair score is >= 70.",
            "- No multi-Awakening chain is represented in the proposal.",
            "",
            "Production migration must not begin unless this proposal and its generated CSVs pass CI.",
        ]
        (DOCS / "HERO_AWAKENING_PAIR_PROPOSAL.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"HERO_AWAKENING_PROPOSAL_OK variants={len(source_keys)} heroes={len(pairs)} awakenings={sum(1 for p in pairs if p.awakening_variant)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
