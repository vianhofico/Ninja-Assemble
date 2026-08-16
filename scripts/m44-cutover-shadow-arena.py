#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / "server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java"
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "stats.resolve(prefix + hero.id(), hero.characterId(), hero.currentVariant(), hero.level(), side, slot)",
        "stats.resolve(prefix + hero.id(), hero.heroId(), hero.awakened(), hero.level(), side, slot)",
    ),
    (
        "new BattleParticipant(unit.id(), hero.characterId(), hero.displayName(), hero.currentVariant(), hero.level(), unit.side(), unit.slot(), unit.maxHp())",
        "new BattleParticipant(unit.id(), hero.characterId(), hero.displayName(), hero.awakened() ? hero.awakeningName() : hero.heroId(), hero.level(), unit.side(), unit.slot(), unit.maxHp())",
    ),
    (
        "new FormationMemberSnapshot(hero.id().toString(), hero.characterId(), hero.currentVariant(), heroPower(hero))",
        "new FormationMemberSnapshot(hero.id().toString(), hero.heroId(), hero.awakened(), heroPower(hero))",
    ),
    (
        "private static long heroPower(OwnedHeroView hero) { return hero.level() * 1_000L + hero.awakeningLevel() * 250L + 500L; }",
        "private static long heroPower(OwnedHeroView hero) { return hero.level() * 1_000L + (hero.awakened() ? 250L : 0L) + 500L; }",
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"M44 Shadow Arena codemod expected exactly one occurrence, got {count}: {old[:80]}")
    text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
print("M44_SHADOW_ARENA_CUTOVER_OK replacements=4")
