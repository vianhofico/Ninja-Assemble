# Mobile Release Status

Current checkpoint: **M18 Unity mobile shell merged; M20 reference evidence harness in progress.**

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
| Art packages marked READY | **0 / 427** | 100% | BLOCKED |
| Reference/balance profiles VERIFIED | **0 / 4** | 100% | BLOCKED |

`scripts/generate-full-art-manifest.py` generates deterministic TODO Addressables contracts for every reference + expanded variant while preserving reviewed rows. Generated contracts are **not** completed art.

M20 registers every release-critical experimental profile and requires real measurement evidence before it can be promoted to `VERIFIED`.

## Code/system status

Implemented foundations include player/account state, wallet/energy, hero catalog, deterministic battle/replay, layered progression/evolution, Main Quest, resource PvE, Arena/Shadow Arena, synergy, Jinchuriki/Tailed Beast, Ninja College/Scroll, inventory/equipment, summon/pity, shops, guild, daily/event objectives, mail, EN/VI runtime localization, Addressables presentation hooks, the M17 playable server/client state loop, and the M18 reproducible Unity mobile scene shell.

## Why mobile is not declared finished yet

The hard release blockers remain real production/research work:

1. **427-variant presentation production** — reviewed high-resolution portrait, icon, 2–2.5-head chibi battle prefab, mandatory animations, skill VFX, SFX/voice hooks and visual-regression captures.
2. **Reference tuning** — combat stats, damage formula, summon profile and level cost remain explicitly experimental until their measurement corpora satisfy M20 evidence thresholds.
3. **Unity production/device pass** — generated shell scenes must be populated with final prefabs/art, exercised on target Android devices and captured by visual regression.

`python scripts/validate-content.py --release`, `python scripts/validate-reference-evidence.py`, and `python scripts/release-audit.py --enforce` are the relevant release checks. The release audit intentionally remains blocked until both art and reference-profile gates are complete.

## Desktop

Desktop production remains gated behind the mobile release. The existing desktop roadmap reuses the same Unity project, complete roster, EN/VI content, deterministic battle protocol and Java backend; Windows x64 remains first priority.
