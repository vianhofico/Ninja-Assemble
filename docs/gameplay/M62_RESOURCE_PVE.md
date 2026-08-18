# M62 Resource PvE

M62 cuts the release Resource PvE catalog over to nine explicit data-driven modes and completes the server/client loop.

## Release modes

1. Naruto Trial
2. Forest Hunt
3. Ninja Test
4. Gold Challenge
5. Daily Food
6. Resource Raid
7. Battle Relief
8. Obito Ultimate Trial
9. Tailed-Beast Conquer

Each mode has EN/VI naming, a five-ninja enemy team, Energy cost, minimum player level, daily UTC attempt cap, Gold/item rewards and `PRODUCTION_READY` release status in `game-data/pve/resource-modes.csv`.

Pre-M62 enum values remain deprecated compatibility symbols for old domain tests only; they are forbidden in the release mode data.

## Runtime loop

`ResourcePveApplicationService` provides:

- board/state endpoint with server UTC game date, attempts remaining, Energy, unlock reason and rewards;
- realtime 5v5 battle using the current saved formation;
- Energy spend and daily attempt accounting in `pve_mode_progress`;
- reward only on victory;
- idempotent Gold/item grants under `resource-pve:<requestId>`;
- request-level PostgreSQL advisory lock;
- exact result persistence in `resource_pve_runs.result_json`, so retries return the original battle/reward result rather than replaying with changed formation/content.

## Unity

Unity now has Resource PvE board/mode/battle DTOs, API calls, store state, recommended playable mode and authoritative state refresh after battle. The production Resource PvE screen is implemented as part of M68 rather than overloading the generic vertical-slice shell.

## Validation

`validate-m62-resource-pve.py --enforce` requires exactly the nine release mode IDs, 45 contiguous enemy slots, production status/reset/resource contracts, idempotency persistence, server/API/client loop and rejects legacy mode types in release data.

The production release gate includes M62 enforcement.
