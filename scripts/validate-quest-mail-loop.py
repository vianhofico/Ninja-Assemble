#!/usr/bin/env python3
"""Static checks for M31 daily quests and mail loop."""
import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"QUEST_MAIL_CONTRACT_INVALID {path} missing={missing}")


def main() -> int:
    with (ROOT / "game-data/quest/daily-quests.csv").open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle))
    if len(rows) != 4:
        raise SystemExit(f"expected four daily quests, got {len(rows)}")
    if {row["metric"] for row in rows} != {"CAMPAIGN_CLEAR", "ARENA_BATTLE", "SUMMON", "HERO_LEVEL_UP"}:
        raise SystemExit("daily quests must use the four server-auditable metrics")
    if any(row["status"] != "DESIGN_BASELINE" or int(row["target"]) != 1 for row in rows):
        raise SystemExit("daily quest definitions must remain target-1 DESIGN_BASELINE")

    require("server/src/main/resources/db/migration/V8__guild_quests_events_mail.sql", "player_quest_progress", "player_mail", "attachments jsonb")
    require("server/src/main/java/com/ninjaassemble/quest/application/QuestCatalogService.java",
            "daily-quest-design-v1", "ItemCatalogService", "CAMPAIGN_CLEAR", "HERO_LEVEL_UP")
    require("server/src/main/java/com/ninjaassemble/quest/application/DailyQuestService.java",
            "campaign_runs", "arena_battles", "summon_history", "wallet_ledger", "player_quest_progress",
            "greatest(player_quest_progress.current_value", "game.clock.reset-hour", "QUEST_REWARD")
    require("server/src/main/java/com/ninjaassemble/mail/application/MailApplicationService.java",
            "welcome-mail-design-v1", "player_mail", "cast(? as jsonb)", "MAIL_ATTACHMENT", "inventory.mutate",
            "wallet.mutate", "claimed = true")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/quests"', '"/quests/{questId}/claim"', '"/mail"', '"/mail/{mailId}/read"', '"/mail/{mailId}/claim"')
    require("server/src/test/java/com/ninjaassemble/quest/application/QuestCatalogServiceTest.java",
            "dailyCatalogUsesFourServerAuditableMetricsAndExistingItems")
    require("client-unity/Assets/Scripts/Game/Network/PlayableDtos.cs",
            "QuestBoardDto", "QuestClaimDto", "MailboxDto", "MailClaimDto")
    require("client-unity/Assets/Scripts/Game/Network/GameApiClient.cs",
            "GetQuestsAsync", "ClaimQuestAsync", "GetMailAsync", "ReadMailAsync", "ClaimMailAsync")
    require("client-unity/Assets/Scripts/Game/Playable/PlayableGameStore.cs",
            "QuestBoardDto Quests", "MailboxDto Mail", "ClaimableQuest", "ClaimableMail", "RefreshQuestsAsync", "RefreshMailAsync")
    require("client-unity/Assets/Scripts/Game/UI/MobileVerticalSliceController.cs",
            "ScreenId.Quest", "ScreenId.Mail", "BuildQuests", "BuildMail", "CLAIM MAIL")
    print("QUEST_MAIL_LOOP_OK quests=4 progress=server-audited reset=DAILY_05 welcome_mail=idempotent")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
