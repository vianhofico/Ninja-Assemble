#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SIM = ROOT / "server/src/main/java/com/ninjaassemble/battle/sim"
REALTIME_ENGINE = SIM / "RealtimeBattleEngine.java"
REALTIME_REQUEST = SIM / "RealtimeBattleRequest.java"
LEGACY_ENGINE = SIM / "DeterministicBattleEngine.java"
LEGACY_REQUEST = SIM / "BattleRequest.java"
RULES = SIM / "BattleRuleset.java"
EVENT = SIM / "BattleEvent.java"
EFFECT = ROOT / "server/src/main/java/com/ninjaassemble/hero/domain/SkillEffectDefinition.java"
SELF = Path(__file__).resolve()


def fail(message: str) -> int:
    print("LEGACY_COMBAT_REMOVAL_FAIL", message)
    return 1


def main() -> int:
    for path in (REALTIME_ENGINE, REALTIME_REQUEST, RULES, EVENT, EFFECT):
        if not path.exists():
            return fail(f"missing {path.relative_to(ROOT)}")

    for path in (LEGACY_ENGINE, LEGACY_REQUEST):
        if path.exists():
            return fail(f"legacy file still exists: {path.relative_to(ROOT)}")

    engine = REALTIME_ENGINE.read_text(encoding="utf-8")
    request = REALTIME_REQUEST.read_text(encoding="utf-8")
    rules = RULES.read_text(encoding="utf-8")
    event = EVENT.read_text(encoding="utf-8")
    effect = EFFECT.read_text(encoding="utf-8")

    for marker in ("continuous-time auto-combat", "PriorityQueue<ScheduledEvent>", "RealtimeBattleRequest", "timestampMs", "ScheduledType"):
        if marker not in engine:
            return fail(f"canonical engine missing realtime marker: {marker}")
    if "record RealtimeBattleRequest" not in request:
        return fail("canonical realtime request record missing")

    for source_name, source in (("engine", engine), ("request", request), ("rules", rules), ("event", event), ("effect", effect)):
        for legacy in ("maxRounds", "durationTurns"):
            if legacy in source:
                return fail(f"{source_name} still contains legacy {legacy}")

    legacy_symbols = ("DeterministicBattleEngine", "BattleRequest")
    scan_roots = [ROOT / "server/src/main", ROOT / "server/src/test", ROOT / "scripts"]
    for scan_root in scan_roots:
        for path in scan_root.rglob("*"):
            if not path.is_file() or path.resolve() == SELF or path.suffix not in {".java", ".py"}:
                continue
            text = path.read_text(encoding="utf-8")
            leaked = [symbol for symbol in legacy_symbols if symbol in text]
            if leaked:
                return fail(f"legacy symbol(s) {leaked} in {path.relative_to(ROOT)}")

    if "long timestampMs" not in event or "long durationMs" not in event:
        return fail("BattleEvent is not fully time-based")
    if "long durationMs" not in effect or "long tickIntervalMs" not in effect:
        return fail("SkillEffectDefinition is not fully time-based")
    if "maxBattleDurationMs" not in rules or "attackIntervalMs" not in rules:
        return fail("BattleRuleset lost realtime duration/interval contract")

    print("LEGACY_COMBAT_REMOVED canonical_engine=RealtimeBattleEngine canonical_request=RealtimeBattleRequest legacy_files=0 legacy_refs=0")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
