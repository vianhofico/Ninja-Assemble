#!/usr/bin/env python3
"""Static contract checks for real-time passive lifecycle execution."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"PASSIVE_LIFECYCLE_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/battle/sim/PassiveTrigger.java",
            "BATTLE_START", "TIME_INTERVAL", "BEFORE_ACTION", "AFTER_ACTION", "AFTER_DAMAGE_TAKEN",
            "AFTER_DAMAGE_DEALT", "HP_THRESHOLD", "ALLY_KO", "STATUS_APPLIED", "SKILL_CAST", "RAGE_SKILL_CAST")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattlePassive.java",
            "oncePerBattle", "thresholdBps", "intervalMs", "List<SkillEffectDefinition> effects")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleUnitSeed.java", "List<BattlePassive> passives")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleEventType.java", "PASSIVE_TRIGGER")
    require("server/src/main/java/com/ninjaassemble/battle/sim/DeterministicBattleEngine.java",
            "triggerPassives", "processPassiveInterval", "firedPassives", "PassiveTrigger.HP_THRESHOLD", "PassiveTrigger.ALLY_KO")
    require("server/src/main/java/com/ninjaassemble/play/domain/PassiveEffectResolver.java",
            "ReferenceProfiles.PASSIVE_LIFECYCLE", "PassiveTrigger.TIME_INTERVAL", "3_000",
            "passive-jinchuriki", "passive-medical", "passive-sharingan", "passive-swordsman", "passive-will-of-fire")
    require("server/src/test/java/com/ninjaassemble/battle/sim/PassiveLifecycleBattleTest.java",
            "battleStartPassiveTriggersAtLogicalTimeZeroAndCanGrantRage", "periodicMedicalPassiveUsesThreeSecondIntervalNotTurnStart",
            "afterDamageTakenPassiveReactsWithoutRecursivePassiveChains", "hpThresholdPassiveFiresOnlyOncePerBattle", "allyKoPassiveTriggersForLivingTeammate")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs", "passiveProfileVersion", "triggerId")
    require("client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs", 'case "PASSIVE_TRIGGER"', "actor.PlayPassive()")
    require("game-data/reference/balance-profiles.csv", "experimental-passive-lifecycle-v1,PASSIVE_LIFECYCLE,EXPERIMENTAL")
    print("PASSIVE_LIFECYCLE_OK model=event_time interval_passives=true once_guard=true")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
