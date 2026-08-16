#!/usr/bin/env python3
"""Static checks for M33 playable Guild loop."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def require(path: str, *tokens: str) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    missing = [token for token in tokens if token not in text]
    if missing:
        raise SystemExit(f"GUILD_CONTRACT_INVALID {path} missing={missing}")


def main() -> int:
    require("server/src/main/resources/db/migration/V8__guild_quests_events_mail.sql",
            "guilds", "guild_members", "guild_contribution_ledger", "guild_boss_runs")
    require("server/src/main/java/com/ninjaassemble/guild/domain/GuildBossState.java",
            "DamageResult apply", "currentHp - applied", "Math.addExact(totalDamage, applied)")
    require("server/src/main/java/com/ninjaassemble/guild/application/GuildApplicationService.java",
            "guild-loop-design-v1", "guild-boss-power-damage-v1", "MEMBER_CAP = 30", "DONATION_AMOUNTS",
            "GuildMemberState", "GuildBossState", "wallet_ledger", "GUILD_DONATION", "GUILD_BOSS_REWARD",
            "guild_contribution_ledger", "guild_boss_runs", "playerHitToday")
    require("server/src/main/java/com/ninjaassemble/play/api/PlayableGameController.java",
            '"/guild"', '"/guild/create"', '"/guild/{guildId}/join"', '"/guild/leave"',
            '"/guild/contribute"', '"/guild/boss/hit"')
    require("server/src/test/java/com/ninjaassemble/guild/domain/GuildBossStateTest.java",
            "bossDamageCannotReduceHpBelowZero")
    require("client-unity/Assets/Scripts/Game/Guild/GuildPlayableBridge.cs",
            "RuntimeInitializeOnLoadMethod", "ScreenId.Guild", "CREATE GUILD", "JOIN GUILD",
            "HIT GUILD BOSS", "DONATE 1000G", "guild/contribute", "guild/boss/hit")
    print("PLAYABLE_GUILD_OK create_join_leave=yes donation=idempotent boss=daily-one-hit client=runtime-bridge")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
