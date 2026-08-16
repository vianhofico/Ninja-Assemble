# Mobile Release Status

Last architecture/content checkpoint: M15 merged; M16 integration/audit in progress.

## Verified content counts

| Gate | Current | Target | State |
|---|---:|---:|---|
| Base playable characters | **189** | >= 180 | PASS |
| Playable variant census | **427** | >= 300 | PASS |
| Bilingual EN/VI techniques | **120** | >= 100 | PASS |
| Reusable gameplay kit profiles | **44** | >= 35 | PASS |
| Base characters mapped to a kit | **189 / 189** | 100% | PASS |
| Major variant kit overrides | **43** | data driven | PASS |
| Art manifest rows | **12 / 427** | 100% | BLOCKED |
| Art packages marked READY | **0 / 427** | 100% | BLOCKED |

These counts are enforced or measured by `scripts/validate-content.py` and `scripts/release-audit.py`.

## Code/system status

Implemented foundations include player/account state, wallet/energy, hero catalog, deterministic battle/replay, layered progression/evolution, Main Quest, resource PvE, Arena/Shadow Arena, synergy, Jinchuriki/Tailed Beast, Ninja College/Scroll, inventory/equipment, summon/pity, shops, guild, daily/event objectives, mail, EN/VI runtime localization and Unity Addressables presentation hooks.

M16 adds packaging of the full `game-data` catalog into the server JAR plus mobile API access to the full roster, variants and bilingual technique kits.

## Why mobile is not declared finished yet

The remaining hard release blocker is **presentation production**, not roster enumeration: every one of the 427 playable variants still needs a reviewed high-resolution portrait, icon, 2–2.5-head chibi battle prefab, mandatory animation states, skill VFX, SFX and visual-regression captures. Existing manifest rows are concept contracts only and are deliberately not marked `READY`.

`python scripts/validate-content.py --release` and `python scripts/release-audit.py --enforce` must remain failing until those assets really exist.

## Desktop

Desktop production remains intentionally gated behind the mobile release. The desktop roadmap reuses the same Unity project, complete roster, EN/VI content, deterministic battle protocol and Java backend; Windows x64 is first priority.
