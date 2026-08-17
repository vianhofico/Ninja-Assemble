#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"PLAYABLE_QUALITY_INVALID {path} missing={missing}")


def forbid(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    leaked = [token for token in tokens if token in text]
    if leaked:
        raise SystemExit(f"PLAYABLE_QUALITY_LEGACY {path} leaked={leaked}")


def main() -> int:
    timeline = "client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs"
    actor = "client-unity/Assets/Scripts/Game/Presentation/BattleActorView.cs"
    hud = "client-unity/Assets/Scripts/Game/Presentation/BattlePlaybackHud.cs"
    feedback = "client-unity/Assets/Scripts/Game/Presentation/BattleImpactFeedback.cs"

    require(timeline,
            "BattlePlaybackHud", "BattleImpactFeedback", "EnsurePlayableQualityLayer",
            "speed != 1 && speed != 2 && speed != 4", "SetPresentationRate", "CurrentTimestampMs")
    require(actor,
            "SetPresentationRate", "RageSkill", "rageSkillClip", "Mathf.MoveTowards",
            "targetHealth01", "targetRage")
    forbid(actor, 'case "ULTIMATE"')
    require(hud,
            "PAUSE", "RESUME", "CreateSpeedButton", "CreateSpeedButton(panel.transform, 4)",
            "timeline.SetPlaybackSpeed", "timeline.SetPaused")
    require(feedback,
            'case "RAGE_FULL"', 'case "RAGE_SKILL_CAST_START"', "RageCinematic",
            "ActorFlash", "RageReadyPulse", "PopLabel", "StartShake")
    require("client-unity/Assets/Scripts/Game/Presentation/BattlePresentationAdapter.cs",
            "TimestampMs = item.timestampMs", "RageAfter = item.rageAfter", "DurationMs = item.durationMs")

    print("PLAYABLE_QUALITY_OK controls=1x,2x,4x pause=1 rage_cinematic=1 impact_feedback=1 smooth_hud=1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
