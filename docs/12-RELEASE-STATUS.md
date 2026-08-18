# Mobile Release Status

Current implementation checkpoint: **M63 — production Arena + Shadow Arena meta-loop.**

Merged/integration foundations now cover M54 battle presentation, M55 Android build pipeline, M57 evidence gates, M58 identity audit, M59 realtime mechanics audit, M60 balance/presentation gate, M61 Campaign, M62 Resource PvE and M63 competitive modes.

## M63 scope

- Arena: 5-unit offense + saved 5-unit defense, async opponents, realtime battle, rating/history, monthly UTC season/reset, idempotent Arena Coin battle and previous-season rewards.
- Shadow Arena: 15-unit saved defense split into three squads, best-of-three realtime series, history, monthly UTC seasons/reset, idempotent Shadow Coin battle and previous-season rewards.
- Canonical Unity `/competitive/...` API/store path uses request IDs; compatibility endpoints remain non-canonical.

## Hard release truth remains unchanged

| Gate | Current | Target |
|---|---:|---:|
| Base skill identity | partial | 970/970 reviewed |
| Awakening identity | partial | 60/60 reviewed |
| Mechanics reviews | 0/1030 | 1030/1030 |
| Balance/presentation reviews | 0/1030 | 1030/1030 |
| Reference profiles VERIFIED | 0/10 | 10/10 |
| Production art fully READY | 0/427 | 427/427 |
| Physical Android device evidence | 0 | >=2 models/classes |

Immediate queue: **M64 Progression → M65 Economy/Live → M66–M68 production UI → M69–M77 art/parity/E2E/device/release**.
