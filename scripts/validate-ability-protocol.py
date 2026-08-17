#!/usr/bin/env python3
"""Static contract checks for deterministic realtime ability/ultimate playback."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"ABILITY_PROTOCOL_INVALID {path} missing={missing}")


def main() -> int:
    require(
        "server/src/main/java/com/ninjaassemble/battle/sim/BattleAbilityKind.java",
        "BASIC", "SKILL1", "SKILL2", "ULTIMATE")
    require(
        "server/src/main/java/com/ninjaassemble/battle/sim/BattleUnitSeed.java",
        "BattleAbilitySet abilities", "BattleAbilitySet.basicOnly")
    require(
        "server/src/main/java/com/ninjaassemble/battle/sim/RealtimeDeterministicBattleEngine.java",
        "nextAbility(", "ability.coefficientBps()", "ability.effectKey()", "energyAfter",
        "CAST_START", "CAST_COMPLETE", "cooldownReadyAtMs")
    require(
        "server/src/main/java/com/ninjaassemble/play/domain/ExperimentalAbilityProfile.java",
        "ReferenceProfiles.ABILITY_CYCLE", "BattleAbilityKind.ULTIMATE", "22_000", "-100",
        "timing.cooldownMs()", "timing.castTimeMs()", "timing.recoveryMs()")
    require(
        "server/src/test/java/com/ninjaassemble/battle/sim/RealtimeDeterministicBattleEngineTest.java",
        "sameSeedProducesExactlySameTimestampedReplay", "combatantsAdvanceOnIndependentSpeedBasedTimelines")
    require(
        "server/src/test/java/com/ninjaassemble/play/domain/ExperimentalAbilityTimingTest.java",
        "productionAbilityCycleUsesExplicitRealtimeTimingForEverySlot")
    require(
        "client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
        "abilityId", "abilityKind", "effectKey", "energyAfter", "timestampMs")
    require(
        "client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs",
        'case "CAST_START"', "PlayAbility(item.AbilityKind)", "SetEnergy(item.EnergyAfter)")
    require(
        "game-data/reference/balance-profiles.csv",
        "experimental-ability-cycle-v1,ABILITY_CYCLE,EXPERIMENTAL")
    print("ABILITY_PROTOCOL_OK realtime deterministic_cycle=basic>skill1>skill2>ultimate explicit_timing=yes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
