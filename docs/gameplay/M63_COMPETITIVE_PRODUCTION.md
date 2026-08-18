# M63 Production Arena + Shadow Arena

M63 completes the competitive meta-loop around the existing realtime battle foundations.

## Arena

The canonical production path uses `/api/v1/play/{playerId}/competitive/arena`.

- five-ninja offense formation;
- saved five-ninja defense snapshot, separate from the mutable Campaign formation;
- five asynchronous opponents sourced from current-season defense profiles;
- request-id advisory lock and exact serialized battle-result replay;
- Arena Coin battle rewards with idempotency keys;
- attack/defense history;
- monthly UTC season IDs and automatic rating reset;
- previous-season settlement + idempotent Arena Coin claim.

## Shadow Arena

- explicit 15-ninja defense roster stored as three five-ninja squads;
- best-of-three realtime squad series;
- saved opponent defense rather than regenerating from current ownership during a retry;
- request-id exact result persistence;
- Shadow Coin rewards;
- attack/defense history;
- monthly UTC season profile and previous-season reward claim.

Default defense is seeded from the current five-ninja formation (Arena) or first 15 owned ninja (Shadow) when a player first enters the mode, and can later be overwritten through the canonical defense endpoints.

## Season model

`CompetitiveSeasonService` derives `arena-YYYY-MM` and `shadow-YYYY-MM` from the server UTC clock. When Arena crosses a month boundary, the previous rating is settled to `competitive_season_results` before the profile resets to 1000. Shadow profiles are already season-keyed and previous rows are settled on demand. Season rewards are persisted and can be claimed once only.

## Compatibility

The pre-M63 `/arena` and `/shadow-arena` endpoints remain for historical compatibility, but Unity's canonical methods now use `/competitive/...` and send request IDs. Production validation rejects a Unity fallback to the old non-idempotent battle path.

## Validation

`validate-m63-competitive.py` reruns the existing Arena and Shadow validators, then enforces defense, monthly season, history, idempotent request persistence, season claim and Unity production-path contracts.
