# M75 — Full E2E and Reliability Certification

M75 defines the release journey that must be exercised against a real environment before RC certification. The structural framework is committed separately from execution evidence so CI cannot manufacture a PASS report.

## Ordered journey

`game-data/release/m75-e2e-journey.json` contains exactly 20 ordered checkpoints from guest login/bootstrap through formation, Campaign/rewards/sweep, leveling, summon, equipment, Resource PvE, Arena, Shadow Arena, Guild, live claims, Awakening, Frame Advance, Advanced Progression, restart persistence and late-game state.

Each step points to an executable client/server contract and required implementation tokens. Normal M75 validation checks those contracts and composes the M74 dashboard.

## Reliability matrix

The ten mandatory reliability concerns are migration chain, transaction boundaries, idempotent retry, duplicate reward prevention, negative-balance prevention, concurrency locking, battle determinism, API contract, restart persistence and soak stability.

The M75 CI workflow also runs the full Maven server regression suite on Java 21.

## Real evidence

Execution evidence belongs under `game-data/release/evidence/m75/` and must conform to `e2e-run.schema.json`. A PASS report must reference the exact commit tested, contain all 20 journey steps and all ten reliability cases with PASS results, and retain server logs, a test report and database migration log.

No synthetic report is committed by M75.

## Commands

Framework/regression gate:

`python scripts/validate-m75-e2e.py`

Release certification:

`python scripts/validate-m75-e2e.py --enforce`

`--enforce` intentionally fails until a real PASS report for the exact release commit exists.
