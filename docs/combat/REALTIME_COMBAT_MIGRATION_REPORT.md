# Real-Time Combat Migration Report — M47/M48

## Executive summary

M47 introduced the deterministic continuous-time combat foundation. M48 begins production cutover by moving campaign PvE to the real-time simulator while retaining a one-way legacy replay projection for existing Unity builds.

The new runtime is simulation-time driven. It never sleeps, never reads the system clock and never allocates one thread/coroutine per combatant. A single priority queue advances all actors, casts and status lifecycle events deterministically.

## Implemented in M47

### Millisecond ability/effect timing

`BattleAbility` owns the canonical real-time timing contract:

- `cooldownMs`
- `castTimeMs`
- `recoveryMs`

`SkillEffectDefinition` now supports:

- `durationMs`
- `tickIntervalMs`

Legacy constructors and `durationTurns` remain temporarily so existing content compiles during migration. Compatibility conversion is not parity-verified timing data.

### Fixed real-time ruleset

`RealtimeBattleRuleset` defines the fixed simulation tick, speed-to-action interval mapping, deterministic timestamp quantization, battle timeout, damage constants and temporary legacy duration bridge.

The current experimental profile uses a 50 ms simulation quantum and a 180 second maximum battle duration.

### Deterministic scheduler

`RealtimeDeterministicBattleEngine` uses one priority queue ordered by:

1. timestamp;
2. scheduled-event priority;
3. stable actor order (`TeamSide`, slot, actor id);
4. insertion sequence.

Scheduled event priority is status tick, status expiry, cast completion, then actor ready. Equal-time behavior is therefore explicit and replay-stable.

### Independent actor timelines and casts

Each alive combatant schedules its own `ACTION_READY` event from speed. Ability execution is split into `CAST_START` and `CAST_COMPLETE`, with cooldown and recovery measured in simulation milliseconds. `STUN` can interrupt a cast at completion and `SILENCE` forces basic attack selection while active.

### Real-time statuses

The new engine stores status expiry as `expiresAtMs`. Periodic effects are scheduled independently and generation tokens invalidate stale refresh events without mutating the priority queue.

Implemented lifecycle includes BURN / POISON / BLEED, STUN, SILENCE, stat modifiers, cleanse/dispel, shields, revive and status expiry events.

### Timestamped replay

`RealtimeBattleEvent` carries `timestampMs` plus millisecond duration metadata. The replay protocol includes `ACTION_READY`, `CAST_START`, `CAST_COMPLETE` and `STATUS_EXPIRED` in addition to damage/heal/status/VFX metadata.

### Foundation tests

`RealtimeDeterministicBattleEngineTest` covers replay determinism, fixed-tick timestamps, independent speed timelines, stable equal-time ordering and millisecond periodic-status lifecycle.

## Implemented in M48

### Canonical application request boundary

`RealtimeBattleRequest` is the application-facing request model for the new simulator. `RealtimeBattleExecutor` isolates the temporary M47 bridge through legacy `BattleRequest`, so campaign application code no longer needs to know about `BattleRuleset.maxRounds`.

The bridge exists only because the M47 engine overload was deliberately additive. It can be removed behind this boundary after all production callers are migrated.

### Campaign PvE is authoritative real-time combat

`PlayableBattleService` no longer instantiates or executes `DeterministicBattleEngine`.

For every campaign wave it now:

1. builds the same player/enemy `BattleUnitSeed` roster;
2. creates `RealtimeBattleRequest` with a deterministic wave seed;
3. executes exactly one real-time simulation;
4. uses `RealtimeBattleResult.outcome` for wave/campaign victory;
5. uses the real-time ruleset version in `campaign_runs`;
6. grants campaign progress/rewards only from the real-time authoritative outcome.

No second legacy simulation is run, avoiding divergent outcomes or duplicate RNG paths.

### Duration-based campaign stars

Campaign stars no longer depend on `BattleResult.rounds`.

The temporary M48 migration gates are:

- 3 stars: at most 20,000 ms per wave;
- 2 stars: at most 40,000 ms per wave;
- 1 star: slower successful clear.

These are experimental migration values, not verified reference balance data.

### Dual replay envelope for Unity compatibility

`RealtimeBattleCompatibilityAdapter` projects the authoritative timestamped replay into the existing `BattleResult` shape. It converts timestamps/durations into approximate legacy round/turn buckets strictly for presentation compatibility.

`PlayableBattleService` now returns both:

- `battle` — legacy projection for existing Unity clients;
- `realtimeBattle` — authoritative timestamped result for the next Unity playback migration.

Each wave also contains both forms. This preserves old JSON properties while allowing M49 to migrate client playback without another server endpoint change.

### M48 tests

Added coverage for:

- projection of timestamped events into the legacy response shape;
- projection of millisecond durations into legacy duration buckets;
- preservation of authoritative outcome/final HP in the projection;
- duration-based 1/2/3-star thresholds and multi-wave scaling.

## Compatibility strategy

The following legacy contracts still exist globally and remain migration debt:

- `DeterministicBattleEngine` for modes not cut over yet;
- `BattleRuleset.maxRounds`;
- `BattleEvent.round`;
- `BattleEvent.durationTurns`;
- `BattleResult.rounds`;
- `SkillEffectDefinition.durationTurns`;
- `PassiveTrigger.TURN_START`;
- the temporary request bridge inside `RealtimeBattleExecutor`;
- the legacy Unity projection returned by campaign PvE.

Campaign progression/rewards no longer depend on those legacy contracts.

## Known non-final defaults

The following values are engineering defaults until reference evidence provides measured parity values:

- 50 ms simulation tick;
- speed/action-interval curve;
- default skill cooldowns;
- default cast/recovery times;
- legacy-turn-to-millisecond conversion;
- default periodic status tick interval;
- 180 second timeout;
- campaign duration star thresholds.

They must not be promoted to `VERIFIED` reference data without evidence.

## Next milestones

### M49 — Unity real-time replay playback

- introduce timestamped battle DTOs in Unity;
- schedule presentation from `timestampMs` rather than round/sequence delay constants;
- interpolate movement/animation between timestamps;
- bind `CAST_START` / `CAST_COMPLETE` to technique animation and VFX;
- keep legacy `battle` fallback during the first client rollout;
- add pause/speed-up/replay without changing simulation results.

### M50 — Arena / Shadow Arena cutover

- migrate standard Arena through the same real-time executor;
- migrate Shadow Arena after 5v5 deterministic/replay tests;
- remove mode-level dependencies on `rounds` and round-based ranking logic;
- preserve seeds and auditable battle records.

### M51 — Content timing cutover

- author `durationMs` and `tickIntervalMs` for all production status effects;
- author cooldown/cast/recovery timing per skill/hero version;
- remove production dependence on legacy duration conversion;
- extend validation to reject turn-authored production content.

### M52 — Legacy engine/projection removal

Delete turn/round contracts only when every production mode and Unity replay uses the real-time protocol and CI covers the complete cutover.

## Release impact

M47/M48 improve combat architecture and move campaign PvE onto the new runtime, but they do not remove the existing production-art, reference-evidence or Android device-validation release blockers. Real-time timing defaults also remain experimental until backed by measured reference evidence.
