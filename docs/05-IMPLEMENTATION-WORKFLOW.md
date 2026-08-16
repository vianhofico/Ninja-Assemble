# Implementation Workflow

## Branch discipline

- one milestone or coherent subsystem per `agent/<scope>` branch;
- draft PR early;
- parity status updated in the same PR as implementation;
- do not merge guessed formulas as `VERIFIED`.

## Feature workflow

1. **Research** — collect screenshots/video/guide evidence.
2. **Specify** — document exact rules and confidence.
3. **Model** — add/update game-data schema.
4. **Implement domain** — deterministic server logic first.
5. **Persist/API** — repository/application layer.
6. **Unity presentation** — screen, animation and VFX playback.
7. **Test** — unit/golden/E2E/visual regression.
8. **Compare** — side-by-side against reference.
9. **Mark parity** — only when evidence matches.

## Coding rules

- no hero-specific branches in generic battle code;
- no magic economy numbers outside versioned game data;
- server timestamps/reset logic use an explicit configured game timezone;
- write idempotent reward grants using transaction IDs;
- all wallet changes create ledger entries in later economy migrations;
- battle inputs use immutable snapshots;
- random behavior uses the supplied deterministic seed;
- presentation does not decide rewards or battle victory.
