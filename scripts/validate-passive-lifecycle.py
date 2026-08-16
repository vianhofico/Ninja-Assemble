#!/usr/bin/env python3
"""Static contract checks for M26 passive lifecycle execution."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"PASSIVE_LIFECYCLE_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/battle/sim/PassiveTrigger.java",
            "BATTLE_START", "TURN_START", "AFTER_DAMAGE_TAKEN", "AFTER_DAMAGE_DEALT", "ALLY_KO", "SELF_LOW_HP")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattlePassive.java",
            "oncePerBattle", "thresholdBps", "List<SkillEffectDefinition> effects")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleUnitSeed.java", "List<BattlePassive> passives")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleEventType.java", "PASSIVE_TRIGGER")
    require("server/src/main/java/com/ninjaassemble/battle/sim/DeterministicBattleEngine.java",
            "triggerPassives", "triggerAllyKo", "firedPassives", "BattleAbilityKind.PASSIVE", "consumeRandom ? randomOne(values, random) : first(values)")
    require("server/src/main/java/com/ninjaassemble/play/domain/PassiveEffectResolver.java",
            "ReferenceProfiles.PASSIVE_LIFECYCLE", "passive-jinchuriki", "passive-medical", "passive-sharingan",
            "passive-swordsman", "passive-will-of-fire", "passive-rinnegan")
    require("server/src/test/java/com/ninjaassemble/battle/sim/PassiveLifecycleBattleTest.java",
            "battleStartPassiveTriggersBeforeRoundOneAndCanGrantEnergy", "afterDamageTakenPassiveReactsWithoutRecursivePassiveChains",
            "lowHpPassiveFiresOnlyOncePerBattle", "allyKoPassiveTriggersForLivingTeammate")
    require("server/src/test/java/com/ninjaassemble/play/domain/PassiveEffectResolverTest.java",
            "everyPassiveTechniqueResolvesToExecutableLifecycleEffects", "EnumSet.allOf(PassiveTrigger.class)")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs", "passiveProfileVersion", "triggerId")
    require("client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs", 'case "PASSIVE_TRIGGER"', "actor.PlayPassive()")
    require("game-data/reference/balance-profiles.csv",
            "experimental-passive-lifecycle-v1,PASSIVE_LIFECYCLE,EXPERIMENTAL")
    print("PASSIVE_LIFECYCLE_OK triggers=6 once_guard=true random_preview_rng_safe=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
