#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-development}"
UNITY_PATH="${UNITY_PATH:-}"
if [[ -z "$UNITY_PATH" ]]; then
  echo "UNITY_PATH must point to the Unity 6000.0 editor executable" >&2
  exit 2
fi

case "$MODE" in
  development) METHOD="NinjaAssemble.EditorTools.MobileBuildAutomation.BuildAndroidDevelopment" ;;
  release) METHOD="NinjaAssemble.EditorTools.MobileBuildAutomation.BuildAndroidRelease" ;;
  *) echo "usage: $0 [development|release]" >&2; exit 2 ;;
esac

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/client-unity"
LOG_DIR="$ROOT/builds/logs"
mkdir -p "$LOG_DIR"

"$UNITY_PATH" \
  -batchmode \
  -nographics \
  -quit \
  -projectPath "$PROJECT" \
  -buildTarget Android \
  -executeMethod "$METHOD" \
  -logFile "$LOG_DIR/unity-android-${MODE}.log"

echo "Android ${MODE} build completed. See builds/android and $LOG_DIR."
