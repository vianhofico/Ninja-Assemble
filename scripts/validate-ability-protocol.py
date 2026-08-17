#!/usr/bin/env python3
"""Static contract checks for continuous-time ability/Rage playback."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"ABILITY_PROTOCOL_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleAbilityKind.java",
            "BASIC", "SKILL1", "SKILL2", "RAGE_SKILL", "AWAKENING_SKILL")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleAbility.java",
            "rageDelta", "cooldownMs", "castTimeMs", "recoveryMs")
    require("server/src/main/java/com/ninjaassemble/battle/sim/BattleUnitSeed.java",
            "BattleAbilitySet abilities", "BattleAbilitySet.basicOnly")
    require("server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleRequest.java",
            "record RealtimeBattleRequest", "BattleRuleset ruleset", "List<BattleUnitSeed> units")
    require("server/src/main/java/com/ninjaassemble/battle/sim/RealtimeBattleEngine.java",
            "PriorityQueue<ScheduledEvent>", "chooseAbility", "RAGE_FULL", "RAGE_SKILL_READY", "attackIntervalMs")
    require("server/src/main/java/com/ninjaassemble/play/domain/ExperimentalAbilityProfile.java",
            "ReferenceProfiles.ABILITY_CYCLE", "BattleAbilityKind.RAGE_SKILL", "22_000", "-100")
    require("server/src/test/java/com/ninjaassemble/battle/sim/RealtimeBattleEngineTest.java",
            "rageCapsAtOneHundredAndUnlocksSignatureRageSkill", "speedChangesIndependentActionFrequency")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
            "timestampMs", "abilityId", "abilityKind", "effectKey", "rageAfter", "durationMs")
    require("client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs",
            "SetPlaybackSpeed", "Time.unscaledDeltaTime", "SetRage(item.RageAfter)")
    require("game-data/combat/rage-rules.csv", "max_rage", "rage_skill_cost", "100")
    require("game-data/reference/balance-profiles.csv", "experimental-ability-cycle-v1,ABILITY_CYCLE,EXPERIMENTAL")
    print("ABILITY_PROTOCOL_OK runtime=continuous_time engine=RealtimeBattleEngine request=RealtimeBattleRequest rage=0..100")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
