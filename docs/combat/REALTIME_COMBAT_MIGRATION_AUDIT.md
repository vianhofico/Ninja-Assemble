# Real-Time Combat Migration Audit

Status: **M49 implementation audit**  
Scope: authoritative server battle runtime, structured effect data, passive lifecycle, APIs/tests and Unity playback.

## Product decision

Combat is continuous-time auto combat. Living actors do not wait for a global turn or round. The server advances a deterministic logical clock by scheduled events; Unity renders those timestamped events and never controls authoritative timing.

## Classification

| Location / concept | Classification | M49 action |
|---|---|---|
| `DeterministicBattleEngine` round loop | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by `PriorityQueue<ScheduledEvent>` ordered by logical timestamp, priority, stable actor order and sequence. |
| Speed sorted once per round | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by `BattleRuleset.attackIntervalMs(effectiveSpeed)` with clamps. |
| `ROUND_START`, `TURN_START`, `TURN_SKIPPED` events | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by `ACTION_READY`, cast/basic/Rage/status events with `timestampMs`. |
| DOT tick at turn start | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by explicit `tickIntervalMs` scheduled events. |
| STUN = skip turn | TURN_BASED_GAMEPLAY_TO_REMOVE | STUN blocks action initiation until `expiresAtMs`; interruptible casts are interrupted. |
| SILENCE measured in turns | TURN_BASED_GAMEPLAY_TO_REMOVE | SILENCE has `durationMs`; basics remain available while active/Rage skills are blocked. |
| `SkillEffectDefinition.durationTurns` | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by `durationMs` + `tickIntervalMs`. |
| Runtime status `remainingTurns` | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by applied/expires/next-tick logical timestamps. |
| `BattleRuleset.maxRounds` | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by `maxBattleDurationMs`. |
| `PassiveTrigger.TURN_START` | TURN_BASED_GAMEPLAY_TO_REMOVE | Periodic intent becomes `TIME_INTERVAL`; reactions remain event triggers. |
| `technique-effects.csv.duration_turns` | TURN_BASED_GAMEPLAY_TO_REMOVE | Replaced by `duration_ms,tick_interval_ms`; each persistent effect reviewed individually. |
| Old tests describing “next turn” | TEST_TO_REWRITE | Replaced by exact timestamp/duration/interval/speed/determinism assertions. |
| Unity DTO `round`, `durationTurns` | TURN_BASED_GAMEPLAY_TO_REMOVE | Migrate to `timestampMs`, `durationMs`, `rageAfter`. |
| Documentation mentioning historical turn-based implementation | DOCUMENTATION_TO_UPDATE | May remain only when clearly marked OLD/deprecated/migration context. |
| English word `return` / unrelated business “turnaround” | LEGITIMATE_NON_COMBAT_USAGE | No blind text replacement. |

## Deterministic scheduling contract

Scheduled events are ordered by:

1. `timestampMs`
2. event priority
3. actor stable order (`side`, `slot`, stable actor id)
4. monotonic schedule sequence

No actor thread is created. No `Thread.sleep`, Unity frame time, system wall clock or collection iteration order participates in combat results.

## Runtime timing concepts

- `nextActionAtMs`
- `cooldownMs`
- `castTimeMs`
- `recoveryMs`
- `actionLockUntilMs`
- `durationMs`
- `tickIntervalMs`
- `expiresAtMs`
- `maxBattleDurationMs`

## Rage foundation

M49 replaces the old generic ultimate-energy cycle with the common Rage/Nộ runtime:

- range `0..100`, clamped;
- Basic attack is baseline universal Rage source;
- at Rage 100, the Rage Skill becomes highest-priority eligible action;
- Rage Skill is not a normal rotating cooldown action;
- exact gains/costs/SPD curve are `EXPERIMENTAL` until evidence is collected;
- M50 owns final per-Hero Version signature Rage Skill research, balance and cinematics.

## Non-goals of this audit

This document does not claim final Ninja Assemble parity values. M49 locks deterministic continuous-time semantics. Reference measurements and full per-Hero Version skill identity are separate evidence/design work and must not be fabricated.
