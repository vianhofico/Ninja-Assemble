#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SIM = ROOT / "server/src/main/java/com/ninjaassemble/battle/sim"
REALTIME_ENGINE = SIM / "RealtimeBattleEngine.java"
REALTIME_REQUEST = SIM / "RealtimeBattleRequest.java"
LEGACY_ENGINE_NAME = "Deterministic" + "BattleEngine"
LEGACY_REQUEST_NAME = "Battle" + "Request"
LEGACY_ENGINE = SIM / (LEGACY_ENGINE_NAME + ".java")
LEGACY_REQUEST = SIM / (LEGACY_REQUEST_NAME + ".java")
RULES = SIM / "BattleRuleset.java"
EVENT = SIM / "BattleEvent.java"
EFFECT = ROOT / "server/src/main/java/com/ninjaassemble/hero/domain/SkillEffectDefinition.java"
SELF = Path(__file__).resolve()


def fail(message: str) -> int:
    print("LEGACY_COMBAT_REMOVAL_FAIL", message)
    return 1


def has_identifier(text: str, symbol: str) -> bool:
    return re.search(r"\b" + re.escape(symbol) + r"\b", text) is not None


def has_standalone_filename(text: str, filename: str) -> bool:
    return re.search(r"(?<![A-Za-z0-9_])" + re.escape(filename) + r"(?![A-Za-z0-9_])", text) is not None


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

    legacy_symbols = (LEGACY_ENGINE_NAME, LEGACY_REQUEST_NAME)
    for scan_root in (ROOT / "server/src/main", ROOT / "server/src/test"):
        for path in scan_root.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            leaked = [symbol for symbol in legacy_symbols if has_identifier(text, symbol)]
            if leaked:
                return fail(f"legacy Java identifier(s) {leaked} in {path.relative_to(ROOT)}")

    legacy_filenames = (LEGACY_ENGINE.name, LEGACY_REQUEST.name)
    for path in (ROOT / "scripts").rglob("*.py"):
        if path.resolve() == SELF:
            continue
        text = path.read_text(encoding="utf-8")
        leaked = [filename for filename in legacy_filenames if has_standalone_filename(text, filename)]
        if leaked:
            return fail(f"validator still opens legacy combat file(s) {leaked} in {path.relative_to(ROOT)}")

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
