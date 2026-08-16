# Mobile Release Status

Current checkpoint: **M17 playable vertical slice merged; M18 generated Unity mobile scene shell in progress.**

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

`scripts/generate-full-art-manifest.py` can now generate deterministic TODO Addressables contracts for every reference + expanded variant while preserving reviewed rows. Generated contracts are **not** counted as completed art.

## Code/system status

Implemented foundations include player/account state, wallet/energy, hero catalog, deterministic battle/replay, layered progression/evolution, Main Quest, resource PvE, Arena/Shadow Arena, synergy, Jinchuriki/Tailed Beast, Ninja College/Scroll, inventory/equipment, summon/pity, shops, guild, daily/event objectives, mail, EN/VI runtime localization, Addressables presentation hooks and the M17 playable server/client state loop.

M18 adds a reproducible Unity Editor scene generator for the full mobile screen shell plus static CI validation. Generated scenes use the live M17 player state for battle, summon and progression exercises.

## Why mobile is not declared finished yet

The hard release blockers remain real production work:

1. **427-variant presentation production** — reviewed high-resolution portrait, icon, 2–2.5-head chibi battle prefab, mandatory animations, skill VFX, SFX/voice hooks and visual-regression captures.
2. **Reference tuning** — M17 deliberately labels combat stats, formulas, summon balance and level cost profiles experimental/unverified until measured against the selected reference build.
3. **Unity production pass** — generated shell scenes must be opened in Unity, populated with final prefabs/art, exercised on target Android devices and captured by visual regression.

`python scripts/validate-content.py --release` and `python scripts/release-audit.py --enforce` must continue to fail until the real release assets and verification evidence exist.

## Desktop

Desktop production remains gated behind the mobile release. The existing desktop roadmap reuses the same Unity project, complete roster, EN/VI content, deterministic battle protocol and Java backend; Windows x64 remains first priority.
