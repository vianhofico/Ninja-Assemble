# Legacy Combat Removal Readiness — M52

## Purpose

M52 prepares the repository to delete the old round-based combat engine safely. It does not delete `DeterministicBattleEngine` yet because deletion changes a large shared Java surface and must be followed by compile/test evidence that is currently not visible through the connected GitHub App.

The goal of this milestone is to make the deletion mechanical rather than architectural: production gameplay, Unity replay, content authoring and static contract validators must no longer require the old engine.

## What is already migrated

### Production battle modes

The authoritative result path is real-time for:

- Campaign / Adventure;
- Arena;
- Shadow Arena squads.

Those application services use `RealtimeBattleExecutor` and `RealtimeBattleRequest`. Rewards, progression, rating and Shadow Arena HP tiebreaks consume `RealtimeBattleResult`.

### Unity presentation

Campaign, Arena and Shadow Arena promote `realtimeBattle` into the shared presentation pipeline and play events by `timestampMs`.

### Production content

Technique, passive and ability profiles author timing explicitly in milliseconds. Production mapping no longer needs `durationTurns`.

## Validator debt removed in M52

Two early combat validators still treated the old engine as the canonical runtime after the M47–M51 migration:

- `scripts/validate-ability-protocol.py`;
- `scripts/validate-passive-lifecycle.py`.

M52 moves both contracts to `RealtimeDeterministicBattleEngine`, real-time tests, explicit ability timing and timestamped Unity presentation.

The M51 structured-effect validator was also already migrated from the old `duration_turns` CSV schema and `DeterministicBattleEngine` to the real-time schema/runtime.

Arena and Shadow Arena contract validators were updated during M50/M51 to reject reintroduction of the legacy engine in competitive application services.

## Removal-readiness gate

`scripts/validate-legacy-combat-removal-readiness.py` scans the repository and fails when:

1. any Java production source other than the legacy engine file itself references `DeterministicBattleEngine`;
2. any Python validator other than the readiness checker itself still requires `DeterministicBattleEngine.java`.

It also reports, without failing yet:

- how many legacy engine test files remain;
- whether `RealtimeBattleExecutor` still contains the temporary `BattleRequest` bridge.

This distinction is intentional. A production/validator reference means deletion is architecturally blocked. Legacy-only tests and the request bridge are the final mechanical cleanup targets.

CI workflow: `.github/workflows/legacy-combat-removal-readiness.yml`.

## Why the engine is not deleted in this milestone

Deleting the old engine immediately would require simultaneous changes to the large simulator source, legacy-only tests, request compatibility overloads and any hidden compile consumers. The connected GitHub integration can create branches/commits/PRs but currently receives HTTP 403 when attempting to read GitHub Actions workflow runs/logs. Therefore M52 establishes explicit deletion preconditions rather than claiming unverified compile success after a destructive cleanup.

## Next deletion sequence

### M53A — Remove request bridge

1. make `RealtimeDeterministicBattleEngine` accept `RealtimeBattleRequest` as its canonical request;
2. make `RealtimeBattleExecutor` call that method directly;
3. migrate `RealtimeDeterministicBattleEngineTest` to `RealtimeBattleRequest`;
4. keep a deprecated legacy overload only if old tests still need it;
5. require readiness output `request_bridge=0`.

### M53B — Delete legacy round engine

After Java compile/test evidence is available:

1. delete legacy-engine-only tests or port any still-useful behavior assertions to real-time tests;
2. delete `DeterministicBattleEngine`;
3. delete `BattleRequest` / `BattleRuleset` only after all remaining references are gone;
4. remove legacy `SkillEffectDefinition.durationTurns` conversion;
5. remove compatibility-only round/turn fields only after Unity no longer requires the projected `BattleResult`.

## Definition of ready

The repository is ready for physical deletion of the old engine when the following are simultaneously true:

- `production_refs=0`;
- `validator_refs=0`;
- `request_bridge=0`;
- all useful old-engine tests have real-time replacements;
- Maven tests pass;
- Unity replay/content static gates pass.

M52 establishes and enforces the first two conditions while making the remaining two visible and measurable.
