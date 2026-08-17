#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SIM = ROOT / "server/src/main/java/com/ninjaassemble/battle/sim"
ENGINE = SIM / "DeterministicBattleEngine.java"
RULES = SIM / "BattleRuleset.java"
EVENT = SIM / "BattleEvent.java"
EFFECT = ROOT / "server/src/main/java/com/ninjaassemble/hero/domain/SkillEffectDefinition.java"
DUPLICATE_ENGINE = SIM / "RealtimeDeterministicBattleEngine.java"


def fail(message: str) -> int:
    print("LEGACY_COMBAT_READINESS_FAIL", message)
    return 1


def main() -> int:
    for path in (ENGINE, RULES, EVENT, EFFECT):
        if not path.exists():
            return fail(f"missing {path.relative_to(ROOT)}")

    if DUPLICATE_ENGINE.exists():
        return fail("parallel RealtimeDeterministicBattleEngine still exists; keep one canonical engine")

    engine = ENGINE.read_text(encoding="utf-8")
    rules = RULES.read_text(encoding="utf-8")
    event = EVENT.read_text(encoding="utf-8")
    effect = EFFECT.read_text(encoding="utf-8")

    for marker in ("continuous-time auto-combat", "PriorityQueue<ScheduledEvent>", "timestampMs", "ScheduledType"):
        if marker not in engine:
            return fail(f"canonical engine missing realtime marker: {marker}")

    for source_name, source in (("engine", engine), ("rules", rules), ("event", event), ("effect", effect)):
        for legacy in ("maxRounds", "durationTurns"):
            if legacy in source:
                return fail(f"{source_name} still contains legacy {legacy}")

    if "long timestampMs" not in event or "long durationMs" not in event:
        return fail("BattleEvent is not fully time-based")
    if "long durationMs" not in effect or "long tickIntervalMs" not in effect:
        return fail("SkillEffectDefinition is not fully time-based")
    if "maxBattleDurationMs" not in rules or "attackIntervalMs" not in rules:
        return fail("BattleRuleset lost realtime duration/interval contract")

    print("LEGACY_COMBAT_REMOVAL_READY canonical_engine=1 round_contracts=0 duplicate_engine=0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
