# Time-Based Skill Audit — M47–M51

## Purpose

Track the migration from round/turn-authored combat to deterministic continuous-time auto combat, including runtime, production battle modes, Unity replay and skill content authoring.

## Canonical real-time timing fields

| Concern | Canonical field | State after M51 |
|---|---|---|
| Ability reuse | `BattleAbility.cooldownMs` | PRODUCTION EXPLICIT |
| Ability wind-up | `BattleAbility.castTimeMs` | PRODUCTION EXPLICIT |
| Post-cast lock | `BattleAbility.recoveryMs` | PRODUCTION EXPLICIT |
| Status lifetime | `SkillEffectDefinition.durationMs` | PRODUCTION EXPLICIT |
| Periodic status cadence | `SkillEffectDefinition.tickIntervalMs` | PRODUCTION EXPLICIT FOR DOT |
| Simulation quantum | `RealtimeBattleRuleset.simulationTickMs` | ACTIVE |
| Battle timeout | `RealtimeBattleRuleset.maxBattleDurationMs` | ACTIVE |
| Replay ordering | `RealtimeBattleEvent.timestampMs` + `sequence` | ACTIVE |

## M51 production content cutover

### Curated technique CSV

`game-data/skills/technique-effects.csv` no longer authors `duration_turns`.

Its timing columns are now:

- `duration_ms`
- `tick_interval_ms`

Every curated runtime `STATUS` row must have `duration_ms > 0`. BURN/POISON/BLEED rows must also have an explicit positive `tick_interval_ms`.

Current values intentionally preserve the earlier experimental timing scale rather than claim reference parity. For example, former 1/2/3-turn statuses map to 3,000/6,000/9,000 ms and curated DOT cadence is explicitly 3,000 ms.

### Generic technique mapping

`TechniqueEffectResolver` now constructs the canonical 10-field `SkillEffectDefinition` with `durationTurns = 0` and explicit millisecond timing. Generic poison/control tag fallbacks also author duration/tick timing directly.

### Passive mapping

`PassiveEffectResolver` no longer authors status duration as turn counts. Current experimental durations are expressed as:

- short buff: 6,000 ms;
- medium buff: 9,000 ms;
- long battle-start buff: 150,000 ms.

The long duration preserves the old experimental 50-turn intent under the temporary 3,000 ms migration scale; it is not reference-verified balance data.

### Ability cycle

`ExperimentalAbilityProfile` now passes `cooldownMs`, `castTimeMs` and `recoveryMs` explicitly into `BattleAbility` rather than relying on compatibility-constructor defaults.

Current explicit values preserve the M47 experimental defaults:

| Slot | Cooldown | Cast | Recovery |
|---|---:|---:|---:|
| Basic | 0 ms | 0 ms | 150 ms |
| Skill 1 | 5,000 ms | 300 ms | 250 ms |
| Skill 2 | 7,000 ms | 300 ms | 250 ms |
| Ultimate | 10,000 ms | 550 ms | 400 ms |

## Runtime and mode state

- One fixed simulation clock; no wall-clock sleeps.
- Every scheduled time is quantized to the configured simulation tick.
- Combatants act independently from speed-derived action intervals.
- Scheduler order is deterministic: **timestamp → event priority → stable actor order → insertion sequence**.
- Status ticks/expiry are scheduled by milliseconds.
- Campaign, Arena and Shadow Arena use authoritative real-time simulation.
- Unity presents those modes from timestamped replay events.

## Validation added in M51

Java tests assert:

- all active technique mappings have `durationTurns == 0`;
- curated/generic DOT/control mappings expose expected millisecond timing;
- passive effects no longer use turn duration;
- all four production ability slots expose explicit cooldown/cast/recovery timing.

`scripts/validate-realtime-content-timing.py` additionally rejects:

- `duration_turns` in the production technique CSV;
- runtime status rows without positive `duration_ms`;
- DOT rows without positive `tick_interval_ms`;
- production resolvers that reference `durationTurns`;
- ability profile code that stops passing explicit timing.

CI: `.github/workflows/realtime-content-timing.yml`.

## Compatibility fields still requiring removal

The following are no longer required by production content authoring but still exist for legacy source/tests/client compatibility:

- `BattleRuleset.maxRounds`;
- `BattleEvent.round`;
- `BattleEvent.durationTurns`;
- `BattleResult.rounds`;
- `SkillEffectDefinition.durationTurns`;
- `SkillEffectDefinition.resolvedDurationMs(...)` legacy bridge;
- `PassiveTrigger.TURN_START` naming;
- `DeterministicBattleEngine` and its round/status lifecycle;
- the temporary `BattleRequest` bridge inside `RealtimeBattleExecutor`;
- `RealtimeBattleCompatibilityAdapter` projected round/turn fields;
- Unity DTO/UI compatibility fields for `round`, `rounds`, `durationTurns`.

## Exit gate for removing legacy combat

M52 may remove legacy turn/round contracts when:

- code search confirms no production battle mode instantiates `DeterministicBattleEngine`;
- production skill/passive/ability content is protected by the M51 explicit timing validator;
- real-time engine accepts the canonical real-time request directly;
- Unity presentation no longer requires projected round/turn fields;
- old engine-only tests are removed/replaced by real-time tests;
- Maven and Unity validation gates pass after deletion.

## Evidence status

M51 completes **authoring-format migration**, not parity verification. Cooldown/cast/recovery/duration/tick values remain `EXPERIMENTAL` until measured reference evidence is captured and promoted through the repository reference/evidence workflow.
