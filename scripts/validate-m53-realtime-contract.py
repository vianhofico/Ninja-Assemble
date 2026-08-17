#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SIM = ROOT / "server/src/main/java/com/ninjaassemble/battle/sim"


def fail(message: str) -> int:
    print("M53_REALTIME_CONTRACT_FAIL", message)
    return 1


def require(path: Path, *tokens: str) -> int | None:
    if not path.exists():
        return fail(f"missing {path.relative_to(ROOT)}")
    text = path.read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        return fail(f"{path.relative_to(ROOT)} missing={missing}")
    return None


def main() -> int:
    engine = SIM / "RealtimeBattleEngine.java"
    request = SIM / "RealtimeBattleRequest.java"
    ability = SIM / "BattleAbility.java"
    event = SIM / "BattleEvent.java"
    kind = SIM / "BattleAbilityKind.java"

    for check in (
        require(engine, "public final class RealtimeBattleEngine", "simulate(RealtimeBattleRequest request)", "PriorityQueue<ScheduledEvent>", "RAGE_SKILL_READY"),
        require(request, "public record RealtimeBattleRequest", "BattleRuleset ruleset", "List<BattleUnitSeed> units"),
        require(ability, "int rageDelta", "cooldownMs", "castTimeMs", "recoveryMs"),
        require(event, "int rageAfter", "long timestampMs", "long durationMs"),
        require(kind, "RAGE_SKILL", "AWAKENING_SKILL"),
    ):
        if check is not None:
            return check

    old_engine = "Deterministic" + "BattleEngine.java"
    old_request = "Battle" + "Request.java"
    for filename in (old_engine, old_request):
        if (SIM / filename).exists():
            return fail(f"obsolete combat contract still exists: {filename}")

    if "ULTIMATE" in kind.read_text(encoding="utf-8"):
        return fail("pre-Rage ULTIMATE runtime alias still exists")
    if "energyDelta" in ability.read_text(encoding="utf-8"):
        return fail("pre-Rage energyDelta compatibility accessor still exists")
    if "energyAfter" in event.read_text(encoding="utf-8"):
        return fail("pre-Rage energyAfter compatibility accessor still exists")

    for relative in (
        "server/src/main/java/com/ninjaassemble/play/application/PlayableBattleService.java",
        "server/src/main/java/com/ninjaassemble/pvp/application/ArenaApplicationService.java",
        "server/src/main/java/com/ninjaassemble/pvp/application/ShadowArenaApplicationService.java",
    ):
        path = ROOT / relative
        check = require(path, "RealtimeBattleEngine", "RealtimeBattleRequest")
        if check is not None:
            return check

    test = ROOT / "server/src/test/java/com/ninjaassemble/battle/sim/RealtimeBattleEngineTest.java"
    check = require(test, "sameSeedProducesIdenticalTimestampedTimeline", "speedChangesIndependentActionFrequency", "rageCapsAtOneHundredAndUnlocksSignatureRageSkill")
    if check is not None:
        return check

    print("M53_REALTIME_CONTRACT_OK engine=RealtimeBattleEngine request=RealtimeBattleRequest legacy_aliases=0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
