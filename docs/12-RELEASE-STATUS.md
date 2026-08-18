# Mobile Release Status

Current implementation checkpoint: **M77 — final release-hardening framework.**

The M54–M77 implementation roadmap has now reached its final milestone on the active M77 branch: playable-quality battle presentation, Android build automation, reference/skill review gates, Campaign, Resource PvE, Arena/Shadow Arena, progression, economy/live systems, production mobile UI, art pipeline/batch ownership, parity verification, E2E/reliability, Android performance/device certification and final release-hardening contracts are all represented in repository code/tooling.

This is **implementation-framework completion**, not public-release certification.

## Final M77 implementation scope

- production HMAC player sessions + Bearer authorization for player-scoped APIs;
- production security headers, Redis-backed rate limiting, secret-only production config and graceful shutdown/health probes;
- VI/EN release metadata and accessibility preferences;
- database backup/restore/rollback + support/incident runbooks;
- licensing/IP provenance, store metadata and operator evidence ledgers;
- strict aggregate certification gate composing M74 parity, M75 E2E, M76 physical-device performance, 427 art packages, production assets, release readiness and release audit;
- no RC tag while any evidence category remains pending.

## Hard release truth

| Gate | Current repository truth | Release target |
|---|---:|---:|
| Base skill identity review | partial | 970/970 |
| Awakening identity review | partial | 60/60 |
| Mechanics reviews | 0/1030 | 1030/1030 |
| Balance/presentation reviews | 0/1030 | 1030/1030 |
| Reference profiles VERIFIED | 0/10 | 10/10 |
| Feature parity census | 0/9 PARITY_PASS | 9/9 |
| Real M75 exact-SHA E2E PASS report | 0 | >=1 |
| Production art fully READY | 0/427 | 427/427 |
| Physical Android passing evidence | 0 | >=2 device models across >=2 classes |
| Signed exact-SHA Release AAB operator evidence | 0 | >=1 |
| Database restore operator evidence | 0 | >=1 PASS |
| Licensing/IP provenance | third-party/IP rights unresolved | all production scopes READY |
| Store privacy/support/review metadata | pending external values | complete |

## Release rule

Do not create an RC/public release tag until both of these pass on the **same exact candidate SHA**:

1. `python scripts/validate-m77-release-hardening.py --enforce`
2. GitHub Actions `production-release-gate`

Desktop implementation starts only after the mobile release evidence gate is genuinely satisfied.
