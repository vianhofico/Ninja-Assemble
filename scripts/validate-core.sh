#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/.tmp-core-classes"
rm -rf "$OUT" && mkdir -p "$OUT"
javac --release 21 -d "$OUT" \
  "$ROOT/server/src/main/java/com/ninjaassemble/battle/domain/DamageChannel.java" \
  "$ROOT/server/src/main/java/com/ninjaassemble/battle/domain/NinjaArchetype.java" \
  "$ROOT/server/src/main/java/com/ninjaassemble/battle/domain/BattleMode.java" \
  "$ROOT/server/src/main/java/com/ninjaassemble/battle/domain/BattleRules.java" \
  "$ROOT/server/src/main/java/com/ninjaassemble/battle/domain/ShadowArenaSeries.java" \
  "$ROOT/server/src/main/java/com/ninjaassemble/progression/domain/FrameTier.java" \
  "$ROOT/server/src/main/java/com/ninjaassemble/progression/domain/FrameProgressionRules.java" \
  "$ROOT/server/src/test/java/com/ninjaassemble/battle/domain/BattleRulesSmokeTest.java"
java -cp "$OUT" com.ninjaassemble.battle.domain.BattleRulesSmokeTest
rm -rf "$OUT"
