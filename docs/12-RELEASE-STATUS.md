# Mobile Release Status

Current checkpoint: **M17 playable vertical slice + M18 Unity mobile shell + M20 reference-evidence gate + M19 component-level art gates merged; M21 Android build/asset-ingest hardening in progress.**

## Verified content counts

| Gate | Current | Target | State |
|---|---:|---:|---|
| Base playable characters | **189** | >= 180 | PASS |
| Playable variant census | **427** | >= 300 | PASS |
| Bilingual EN/VI techniques | **120** | >= 100 | PASS |
| Reusable gameplay kit profiles | **44** | >= 35 | PASS |
| Base characters mapped to a kit | **189 / 189** | 100% | PASS |
| Major variant kit overrides | **43** | data driven | PASS |
| Existing reviewed/concept art manifest rows | **12 / 427** | 100% | BLOCKED |
| Art packages marked overall READY | **0 / 427** | 100% | BLOCKED |
| Component-level art packages tracked | **12 / 427** | 100% | BLOCKED |
| Component-level art packages complete | **0 / 427** | 100% | BLOCKED |
| Reference/balance profiles VERIFIED | **0 / 4** | 100% | BLOCKED |
| Passing Android device evidence | **0** | >= 2 across >= 2 models/classes | BLOCKED |

Generated TODO rows are never counted as completed art. Device evidence remains header-only until a real build is exercised on real Android hardware.

## Code/system status

Implemented foundations include player/account state, wallet/energy, hero catalog, deterministic battle/replay, layered progression/evolution, Main Quest, resource PvE, Arena/Shadow Arena, synergy, Jinchuriki/Tailed Beast, Ninja College/Scroll, inventory/equipment, summon/pity, shops, guild, daily/event objectives, mail, EN/VI runtime localization, Addressables presentation hooks, the M17 playable state loop and the M18 reproducible Unity mobile scene shell.

M19 prevents a variant from being globally READY while a required art component is unfinished. M20 prevents development balance values from being mislabeled reference-verified without measured evidence. M21 adds Android APK/AAB build automation, concrete file-backed art package validation and device-release evidence gates.

## Why mobile is not declared finished yet

The hard release blockers remain real production/research/device work:

1. **427-variant presentation production** — high-resolution portrait, icon, 2–2.5-head chibi battle prefab, mandatory animations, skill VFX, SFX/voice hooks and visual-regression captures.
2. **Reference tuning** — combat stats, damage formula, summon profile and level cost remain experimental until their measurement corpora satisfy M20 thresholds.
3. **Android production pass** — build an APK/AAB with Unity Android support, run it on at least two distinct device models/classes, and record passing smoke/performance evidence.

Release gates intentionally remain blocked until those inputs are real.

## Desktop

Desktop implementation remains gated behind mobile release. The existing desktop roadmap will reuse this Unity project, complete roster, EN/VI content, deterministic battle protocol and Java backend; Windows x64 remains the first target after mobile passes release audit.
