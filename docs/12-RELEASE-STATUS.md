# Mobile Release Status

Current implementation checkpoint: **M61 — production Campaign vertical slice.**

Merged/integration foundations now cover M54 battle presentation, M55 Android build pipeline, M57 evidence gates, M58 identity audit, M59 realtime mechanics audit, M60 balance/presentation gate, and M61 Campaign.

## M61 release scope

- 12 release Campaign stages: 4 NORMAL + 4 ELITE + 4 HEROIC.
- prerequisite chain, Energy gates, first/repeat EXP/currency rewards.
- item rewards and 5-enemy waves; chapter finals have two waves.
- campaign progress, clear count, best stars and first-clear state.
- idempotent Campaign sweep with repeat rewards and no fake battle result.
- Unity API/store sweep contract ready for the production Campaign screen in M67.

## Release truth still blocked

| Gate | Current | Target |
|---|---:|---:|
| Base skill identity | partial | 970/970 reviewed |
| Awakening identity | partial | 60/60 reviewed |
| Mechanics reviews | 0/1030 | 1030/1030 |
| Balance/presentation reviews | 0/1030 | 1030/1030 |
| Reference profiles VERIFIED | 0/10 | 10/10 |
| Production art fully READY | 0/427 | 427/427 |
| Physical Android evidence | 0 | >=2 models/classes |

M61 Campaign is an internal product release census; it does not fabricate external parity evidence. Hard review/evidence/art/device gates remain unchanged.

Immediate queue: **M62 Resource PvE → M63 Arena/Shadow Arena → M64 Progression → M65 Economy/Live → M66–M68 production UI → M69–M77 art/parity/E2E/device/release**.
