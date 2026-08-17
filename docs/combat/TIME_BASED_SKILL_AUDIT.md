# Time-Based Skill Audit — M47

## Purpose

Track the migration from round/turn authored combat to deterministic continuous-time auto combat. M47 introduces the real-time runtime and compatibility contracts; it does **not** declare the legacy round engine removed yet.

## New canonical timing fields

| Concern | Canonical real-time field | M47 state |
|---|---|---|
| Ability reuse | `BattleAbility.cooldownMs` | IMPLEMENTED |
| Ability wind-up | `BattleAbility.castTimeMs` | IMPLEMENTED |
| Post-cast lock | `BattleAbility.recoveryMs` | IMPLEMENTED |
| Status lifetime | `SkillEffectDefinition.durationMs` | IMPLEMENTED |
| Periodic status cadence | `SkillEffectDefinition.tickIntervalMs` | IMPLEMENTED |
| Simulation quantum | `RealtimeBattleRuleset.simulationTickMs` | IMPLEMENTED |
| Battle timeout | `RealtimeBattleRuleset.maxBattleDurationMs` | IMPLEMENTED |
| Replay ordering | `RealtimeBattleEvent.timestampMs` + `sequence` | IMPLEMENTED |

## Temporary compatibility fields that still require cutover

These are intentionally retained so existing campaign/PvP/content callers continue compiling while migration proceeds:

- `BattleRuleset.maxRounds`
- `BattleEvent.round`
- `BattleEvent.durationTurns`
- `BattleResult.rounds`
- `SkillEffectDefinition.durationTurns`
- `PassiveTrigger.TURN_START`
- `DeterministicBattleEngine` round loop and `advanceStatuses()` lifecycle

`SkillEffectDefinition.resolvedDurationMs(...)` is a temporary bridge only. New or retuned content must author milliseconds directly rather than relying on turn conversion.

## Runtime semantics in M47

- One fixed simulation clock; no wall-clock sleeps.
- Every scheduled time is quantized to the configured simulation tick.
- Combatants act independently from speed-derived action intervals.
- Scheduler order is deterministic: **timestamp → event priority → stable actor order → insertion sequence**.
- Status ticks and expiry are scheduled events, not turn-start hooks.
- Ability casts can be interrupted by `STUN` before cast completion.
- `SILENCE` forces basic-attack selection while active.
- Cooldowns use simulation time and never system time.
- Unity-facing replay now has timestamped `ACTION_READY`, `CAST_START`, `CAST_COMPLETE`, status tick/expiry and damage events.

## Remaining audit work

1. Migrate campaign, resource PvE, Arena and Shadow Arena entry points from `DeterministicBattleEngine` to `RealtimeDeterministicBattleEngine`.
2. Replace API/DTO fields that expose `round`/`rounds` with duration/timestamp-compatible fields.
3. Update Unity replay DTOs and playback to interpolate by `timestampMs`.
4. Convert all skill/status content from `durationTurns` to measured `durationMs` and `tickIntervalMs`.
5. Replace `PassiveTrigger.TURN_START` with action/time based trigger naming after all content maps are migrated.
6. Remove `maxRounds`, legacy duration conversion and the old engine only after all server modes and Unity replay tests use the real-time protocol.

## Exit gate for removing legacy combat

Legacy turn/round code may be deleted only when:

- no production battle mode instantiates `DeterministicBattleEngine`;
- no production content requires `durationTurns`;
- Unity consumes timestamped replay events;
- deterministic replay tests cover equal-time ordering, speed timelines, casts, cooldowns, control, DOT/HOT lifecycle, revive and timeout;
- server CI and Unity shell validation pass with the legacy engine excluded.
