#!/usr/bin/env python3
"""Static contract checks for M24 structured battle effects."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"STRUCTURED_EFFECTS_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/hero/domain/EffectType.java",
            "DAMAGE", "HEAL", "ENERGY", "STATUS", "CLEANSE", "DISPEL", "REVIVE", "SHIELD")
    require("server/src/main/java/com/ninjaassemble/hero/domain/TargetSelector.java",
            "SELF", "FRONTMOST_ENEMY", "RANDOM_ENEMY", "ALL_ENEMIES", "LOWEST_HP_ALLY", "ALL_ALLIES")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleAbility.java", "List<SkillEffectDefinition> effects")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleEventType.java",
            "HEAL", "SHIELD_ABSORB", "STATUS_APPLIED", "STATUS_TICK", "STATUS_CLEANSED", "REVIVE", "TURN_SKIPPED")
    require("server/src/main/java/com/ninjaassemble/battle/sim/DeterministicBattleEngine.java",
            "DOT_STATUSES", "hasStatus(\"STUN\")", "hasStatus(\"SILENCE\")", "case HEAL", "case SHIELD",
            "case CLEANSE", "case DISPEL", "case REVIVE", "TargetSelector.FRONTMOST_ENEMY")
    require("server/src/test/java/com/ninjaassemble/battle/sim/StructuredBattleEffectsTest.java",
            "areaDamageHitsEveryLivingEnemyAndShieldAbsorbsLaterDamage", "stunSkipsTheTargetsNextScheduledTurn",
            "burnTicksAtTheStartOfTheAffectedUnitsTurn", "BattleEventType.REVIVE")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs", "effectType", "statusId", "durationTurns")
    require("client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs",
            "STATUS_TICK", "SHIELD_ABSORB", "STATUS_APPLIED", "STATUS_CLEANSED", "REVIVE")
    require("game-data/skills/technique-effects.csv",
            "technique_id,effect_index,effect_type,target_selector,channel,coefficient_bps,flat_amount,status_id,chance_bps,duration_turns")
    require("game-data/reference/balance-profiles.csv",
            "experimental-structured-effects-v1,STRUCTURED_EFFECTS,EXPERIMENTAL")
    print("STRUCTURED_EFFECTS_OK damage heal shield energy status cleanse dispel revive dot cc")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
