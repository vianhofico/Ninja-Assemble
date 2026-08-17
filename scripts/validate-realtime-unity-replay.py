#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FILES = {
    "dtos": ROOT / "client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
    "compat": ROOT / "client-unity/Assets/Scripts/Game/Network/RealtimeBattleDtoCompatibility.cs",
    "store": ROOT / "client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
    "event": ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattlePresentationEvent.cs",
    "adapter": ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattlePresentationAdapter.cs",
    "timeline": ROOT / "client-unity/Assets/Scripts/Game/Presentation/BattleTimelinePlayer.cs",
}

REQUIRED_MARKERS = {
    "dtos": ["RealtimeBattleEventDto", "RealtimeBattleResultDto", "timestampMs", "durationMs", "realtimeBattle", "ArenaBattleDto", "ShadowSquadBattleDto"],
    "compat": ["Promote(PlayBattleDto result)", "Promote(ArenaBattleDto result)", "Promote(ShadowArenaBattleDto result)", "Project(BattleResultDto fallback", "Array.Sort(events, CompareEvents)", "realtime = true"],
    "store": ["RealtimeBattleDtoCompatibility.Promote(await api.PlayCampaignStageAsync", "RealtimeBattleDtoCompatibility.Promote(await api.FightArenaAsync", "RealtimeBattleDtoCompatibility.Promote(await api.FightShadowArenaAsync"],
    "event": ["TimestampMs", "DurationMs", "IsRealtime"],
    "adapter": ["TimestampMs = item.timestampMs", "DurationMs = item.durationMs", "battle.realtime || item.realtime"],
    "timeline": ["PlaybackSpeed", "item.TimestampMs - previousRealtimeTimestampMs", 'case "CAST_START"', 'case "STATUS_EXPIRED"', "if (!item.IsRealtime) actor.PlayAbility"],
}


def main() -> int:
    missing = [str(path.relative_to(ROOT)) for path in FILES.values() if not path.exists()]
    if missing:
        print("REALTIME_UNITY_REPLAY_MISSING_FILES", ", ".join(missing))
        return 1

    for key, path in FILES.items():
        content = path.read_text(encoding="utf-8")
        for marker in REQUIRED_MARKERS[key]:
            if marker not in content:
                print("REALTIME_UNITY_REPLAY_MISSING_MARKER", key, marker)
                return 1

    timeline = FILES["timeline"].read_text(encoding="utf-8")
    if "Thread.Sleep" in timeline or "Task.Delay" in timeline:
        print("REALTIME_UNITY_REPLAY_WALLCLOCK_BLOCKING")
        return 1

    print("REALTIME_UNITY_REPLAY_OK campaign=1 arena=1 shadow=1 timestamp_player=1 legacy_fallback=1")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
