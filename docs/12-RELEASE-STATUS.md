# Mobile Release Status

Current checkpoint: **M18 Unity mobile shell merged, M20 evidence-backed reference tuning gate merged, M19 component-level art package gates in progress.**

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

The 12 tracked flagship packages currently have portrait/icon concepts only; chibi prefab, animation, VFX, SFX, regression capture and final review remain unfinished. Generated TODO rows are never counted as completed art.

## Code/system status

Implemented foundations include player/account state, wallet/energy, hero catalog, deterministic battle/replay, layered progression/evolution, Main Quest, resource PvE, Arena/Shadow Arena, synergy, Jinchuriki/Tailed Beast, Ninja College/Scroll, inventory/equipment, summon/pity, shops, guild, daily/event objectives, mail, EN/VI runtime localization, Addressables presentation hooks, M17 playable server/client state loop and M18 reproducible Unity mobile scene shell.

M20 prevents experimental combat stats, damage formulas, summon balance and level costs from being mislabeled as reference-verified without structured measurements.

M19 adds per-component art gates so a variant cannot be labeled READY while missing animation/VFX/SFX/capture/review work.

## Why mobile is not declared finished yet

The hard release blockers remain real production/research work:

1. **427-variant presentation production** — reviewed high-resolution portrait, icon, 2–2.5-head chibi battle prefab, mandatory animations, skill VFX, SFX/voice hooks and visual-regression captures.
2. **Reference tuning** — combat stats, damage formula, summon profile and level cost remain explicitly experimental until their measurement corpora satisfy M20 evidence thresholds.
3. **Unity production/device pass** — generated shell scenes must be populated with final prefabs/art, exercised on target Android devices and captured by visual regression.

Release gates intentionally remain blocked until those inputs are real.

## Desktop

Desktop production remains gated behind the mobile release. The existing desktop roadmap reuses the same Unity project, complete roster, EN/VI content, deterministic battle protocol and Java backend; Windows x64 remains first priority.
