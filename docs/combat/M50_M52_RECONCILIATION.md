# M50–M52 Reconciliation Into Main

## Context

`main` already contained the M49 deterministic continuous-time Rage combat core before the M50/M51/M52 side branches were merged. Those side branches were developed on a parallel realtime migration path that introduced a second engine/request/replay compatibility layer. Merging them verbatim would have regressed the newer M49 contracts on `main`.

This reconciliation preserves the intent of the requested branches while keeping the M49 architecture authoritative.

## M50 skill design

`agent/m50-skill-design` was merged directly through PR #60. The five-slot Rage Skill design/audit model and reviewed Hero Version skill design work are therefore part of `main`.

## M50 competitive realtime cutover

Arena and Shadow Arena continue to use `DeterministicBattleEngine`, whose implementation on `main` is already the authoritative continuous-time scheduler. No parallel `RealtimeDeterministicBattleEngine` or replay adapter is introduced.

The reconciled M50 work adds explicit validation that:

- the shared engine is continuous-time and priority-queue driven;
- Arena and Shadow Arena both use the shared realtime ruleset/result path;
- Unity consumes timestamp/duration replay data directly;
- competitive validation fails if round-based contracts are reintroduced.

## M51 explicit timing

`main` already uses `duration_ms` / `tick_interval_ms`, explicit `cooldownMs` / `castTimeMs` / `recoveryMs`, and Rage Skill semantics. The reconciliation therefore keeps the newer values already present on `main` and adds the M51 CI gate without restoring compatibility fields such as `durationTurns`.

The timing gate checks curated status duration, DOT tick cadence, ability timing fields, and the Rage Skill mapping.

## M52 cleanup/readiness

On `main`, `DeterministicBattleEngine` is no longer a legacy turn engine: M49 converted that class in place into the canonical continuous-time engine. Therefore the side-branch M52 rule "remove DeterministicBattleEngine" is not applicable.

The reconciled readiness gate instead enforces the architectural goal:

- exactly one canonical realtime engine;
- no duplicate `RealtimeDeterministicBattleEngine`;
- no `maxRounds` or `durationTurns` contracts in the canonical simulation model;
- timestamp/duration-based battle events and status effects;
- realtime duration/interval rules remain present.

## Result

The branch histories are intentionally reconciled by behavior rather than copied verbatim where they conflict with the newer M49 Rage runtime. This keeps the desired M50–M52 functionality while avoiding a rollback of the current `main` combat architecture.
