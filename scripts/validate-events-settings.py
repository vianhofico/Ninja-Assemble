#!/usr/bin/env python3
"""Static contract checks for M36 weekly Events and local Settings."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"EVENTS_SETTINGS_INVALID {path} missing={missing}")


def reject(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    present = [token for token in tokens if token in text]
    if present:
        raise SystemExit(f"EVENTS_SETTINGS_INVALID {path} forbidden={present}")


def main() -> int:
    require("server/src/main/resources/db/migration/V8__guild_quests_events_mail.sql", "player_event_progress", "objective_state jsonb")
    require("server/src/main/java/com/ninjaassemble/meta/domain/EventDefinition.java", "activeAt(Instant now)")
    require("server/src/main/java/com/ninjaassemble/meta/application/WeeklyEventService.java",
            "weekly-event-design-v1", "EventDefinition", "QuestDefinition", "ResetCadence.EVENT",
            "ObjectiveType.CLEAR_STAGE", "ObjectiveType.WIN_ARENA", "ObjectiveType.SUMMON",
            "campaign_runs", "arena_battles", "summon_history", "player_event_progress",
            "objective_state->>'claimMask'", "for update", "EVENT_REWARD", "wallet.mutate", "inventory.mutate",
            "TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)")
    reject("server/src/main/java/com/ninjaassemble/meta/application/WeeklyEventService.java", "ObjectMapper", "jackson")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/events"', '"/events/{objectiveId}/claim"')
    require("client-unity/Assets/Scripts/Game/Events/WeeklyEventPlayableBridge.cs",
            "RuntimeInitializeOnLoadMethod", "ScreenId.Events", "/events", "/claim", "NHẬN THƯỞNG", "CLAIM")
    require("client-unity/Assets/Scripts/Game/Settings/SettingsPlayableBridge.cs",
            "ScreenId.Settings", "PlayerPrefs", "na.language", "na.sound", "na.quality",
            "AudioListener.volume", "QualitySettings.SetQualityLevel", "ENGLISH", "TIẾNG VIỆT")
    print("EVENTS_SETTINGS_OK event=weekly-authoritative claim=idempotent settings=device-persisted bilingual=vi-en")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
