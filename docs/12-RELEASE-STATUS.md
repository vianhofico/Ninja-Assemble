# Mobile Release Status

Current checkpoint: **M17 playable vertical slice, M18 Unity mobile shell, M19 component art gates, M20 evidence-backed reference tuning gate and M21 Android build/asset-ingest contracts merged; M22 runtime 5v5 visual stage in progress.**

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

Implemented foundations include player/account state, wallet/energy, hero catalog, deterministic battle/replay, layered progression/evolution, Main Quest, resource PvE, Arena/Shadow Arena, synergy, Jinchuriki/Tailed Beast, Ninja College/Scroll, inventory/equipment, summon/pity, shops, guild, daily/event objectives, mail, EN/VI runtime localization, Addressables presentation hooks, the M17 playable state loop, M18 reproducible Unity mobile scene shell and M21 APK/AAB build automation contracts.

M19 prevents a variant from being globally READY while a required art component is unfinished. M20 prevents development balance values from being mislabeled reference-verified without measured evidence. M21 requires concrete files behind future READY art states and real Android smoke/performance evidence.

M22 adds participant metadata and a runtime 5v5 stage that can load READY chibi Addressables or use an explicit fallback, then play ATTACK/DAMAGE/KO events with HP, damage numbers, critical feedback and victory state.

## Why mobile is not declared finished yet

The hard release blockers remain real production/research/device work:

1. **427-variant presentation production** — high-resolution portrait, icon, 2–2.5-head chibi battle prefab, mandatory animations, skill VFX, SFX/voice hooks and visual-regression captures.
2. **Reference tuning** — combat stats, damage formula, summon profile and level cost remain experimental until their measurement corpora satisfy M20 thresholds.
3. **Skill/ultimate event protocol** — current M22 visual timeline is generic ATTACK/DAMAGE/KO. M23 must add stable ability/effect identifiers for character-specific skill/ultimate animation and VFX playback.
4. **Android production pass** — build APK/AAB with Unity Android support, run on at least two distinct device models/classes, and record passing smoke/performance evidence.

Release gates intentionally remain blocked until those inputs are real.

## Desktop

Desktop implementation remains gated behind mobile release. The existing desktop roadmap will reuse this Unity project, complete roster, EN/VI content, deterministic battle protocol and Java backend; Windows x64 remains the first target after mobile passes release audit.
