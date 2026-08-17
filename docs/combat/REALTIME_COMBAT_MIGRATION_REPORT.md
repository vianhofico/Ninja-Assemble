# Real-Time Combat Migration Report — M47

## Executive summary

M47 introduces the first production-grade foundation for deterministic continuous-time auto combat without deleting the existing round-based engine. The migration is intentionally additive: existing campaign/PvP callers can continue using `DeterministicBattleEngine` while new callers can adopt `RealtimeDeterministicBattleEngine` and the timestamped replay protocol incrementally.

The new runtime is simulation-time driven. It never sleeps, never reads the system clock and never allocates one thread/coroutine per combatant. A single priority queue advances all actors, casts and status lifecycle events deterministically.

## Implemented in M47

### 1. Millisecond ability timing

`BattleAbility` now owns the canonical real-time timing contract:

- `cooldownMs`
- `castTimeMs`
- `recoveryMs`

Legacy constructors remain available so existing content compiles during migration. Their default timings are migration defaults only, not parity-verified reference values.

### 2. Millisecond effect timing

`SkillEffectDefinition` now supports:

- `durationMs`
- `tickIntervalMs`

`durationTurns` remains temporarily for compatibility. `resolvedDurationMs(...)` converts legacy data only when no real-time duration is authored.

### 3. Fixed real-time ruleset

`RealtimeBattleRuleset` defines:

- fixed simulation tick;
- speed-to-action interval mapping;
- minimum/maximum action interval;
- deterministic timestamp quantization;
- battle timeout;
- temporary legacy-turn conversion duration;
- default periodic status interval;
- damage/critical constants reused by the simulator.

The current experimental profile uses a 50 ms simulation quantum and a 180 second maximum battle duration.

### 4. Deterministic scheduler

`RealtimeDeterministicBattleEngine` uses one `PriorityQueue` ordered by:

1. timestamp;
2. scheduled-event priority;
3. stable actor order (`TeamSide`, slot, actor id);
4. insertion sequence.

Current scheduled event priority is:

1. status tick;
2. status expiry;
3. cast completion;
4. actor ready.

This ordering is explicit so equal-time behavior is replay-stable and testable.

### 5. Independent actor timelines

Each alive combatant schedules its own `ACTION_READY` event from speed. A faster ninja can therefore act more frequently without waiting for a global round boundary.

The migration ruleset maps speed 100 to the base action interval and applies deterministic clamping/quantization. Speed modifiers are evaluated when the next action is scheduled.

### 6. Cast/cooldown/recovery lifecycle

A ready actor selects an executable ability, pays/receives its energy delta, starts its cooldown and emits `CAST_START`.

The effect resolves only at scheduled `CAST_COMPLETE`. `STUN` present at cast completion interrupts the action. After resolution/interruption, the next ready time includes recovery plus the actor's current speed-derived interval.

`SILENCE` forces basic attack selection while active.

### 7. Real-time statuses

Statuses store `expiresAtMs` rather than remaining turn count in the new engine. Periodic effects are individual scheduled events. Refresh/replacement uses generation tokens so stale tick/expiry entries can remain in the heap safely and become no-ops when popped.

Implemented real-time lifecycle includes:

- BURN / POISON / BLEED periodic damage;
- STUN;
- SILENCE;
- stat up/down modifiers;
- cleanse/dispel;
- shield interaction;
- revive and action rescheduling;
- status expiry replay event.

### 8. Timestamped replay protocol

`RealtimeBattleEvent` adds `timestampMs` and millisecond duration metadata. New replay event types include:

- `ACTION_READY`
- `CAST_START`
- `CAST_COMPLETE`
- `STATUS_EXPIRED`

Existing ability/effect/status/VFX identifiers are retained so Unity can map gameplay events to presentation assets.

### 9. Determinism tests

`RealtimeDeterministicBattleEngineTest` covers the first critical guarantees:

- identical seed/input -> identical timestamped replay;
- every emitted timestamp is quantized to the fixed simulation tick;
- independent speed-based timelines;
- stable ordering for equal timestamps;
- millisecond periodic-status tick and expiry behavior.

The existing round-engine tests remain in place during the bridge period.

## Compatibility strategy

M47 deliberately does not alter production battle entry points yet. This avoids changing campaign, Arena, Shadow Arena and resource PvE behavior in the same milestone that introduces the new scheduler.

The following legacy contracts still exist:

- `DeterministicBattleEngine`
- `BattleRuleset.maxRounds`
- `BattleEvent.round`
- `BattleEvent.durationTurns`
- `BattleResult.rounds`
- `SkillEffectDefinition.durationTurns`
- `PassiveTrigger.TURN_START`

These are migration debt, not desired final architecture.

## Known non-final defaults

The following values are engineering defaults until reference evidence provides measured parity values:

- 50 ms simulation tick;
- speed/action-interval curve;
- default skill cooldowns;
- default cast/recovery times;
- legacy-turn-to-millisecond conversion;
- default periodic status tick interval;
- 180 second timeout.

They must not be promoted to `VERIFIED` reference data without evidence.

## Recommended next milestones

### M48 — Server battle-mode cutover

- create a common battle-simulation facade;
- migrate Main Quest / resource PvE first;
- migrate Arena next;
- migrate Shadow Arena after 5v5 parity tests;
- preserve deterministic seed/replay contract at every boundary;
- add mode-level real-time integration tests.

### M49 — Unity real-time replay playback

- introduce timestamped battle DTOs in Unity;
- schedule presentation from `timestampMs` rather than round/sequence delay constants;
- interpolate movement/animation between event timestamps;
- bind `CAST_START` / `CAST_COMPLETE` to technique animation and VFX;
- add pause/speed-up/replay support without modifying simulation results.

### M50 — Content timing cutover

- author `durationMs` and `tickIntervalMs` for all status effects;
- author cooldown/cast/recovery timing per skill/hero version;
- remove production dependence on compatibility conversions;
- extend validation to reject turn-authored production content.

### M51 — Legacy engine removal

Delete turn/round contracts only when every production mode, Unity replay and content row uses the real-time protocol and CI covers the cutover.

## Release impact

M47 improves architecture but does not remove the existing release blockers around production art, reference evidence and Android device validation. It also must not be interpreted as gameplay parity for real-time timings until those values have measured reference evidence.
