# Real-Time Combat Migration Report — M47–M50

## Executive summary

M47 introduced deterministic continuous-time combat. M48 moved Campaign PvE to it. M49 moved Unity campaign playback to timestamped replay. M50 moves Arena and Shadow Arena to the same authoritative runtime and promotes their replay in Unity.

The simulator is driven only by simulation time: no sleeps, no system clock and no per-combatant thread/coroutine. One priority queue advances actors, casts and status lifecycle events deterministically.

## M47 — Runtime foundation

Implemented canonical millisecond combat timing:

- `BattleAbility.cooldownMs`
- `BattleAbility.castTimeMs`
- `BattleAbility.recoveryMs`
- `SkillEffectDefinition.durationMs`
- `SkillEffectDefinition.tickIntervalMs`
- `RealtimeBattleRuleset.simulationTickMs`

`RealtimeDeterministicBattleEngine` uses deterministic ordering:

1. timestamp;
2. scheduled-event priority;
3. stable actor order (`TeamSide`, slot, actor id);
4. insertion sequence.

The current scheduled priorities are status tick, status expiry, cast completion, then actor ready. Actors advance on independent speed-derived timelines. STUN/SILENCE, DOT lifecycle, stat modifiers, shield, cleanse/dispel and revive are time-based in the new engine.

`RealtimeBattleEvent`/`RealtimeBattleResult` provide timestamped replay and final HP.

## M48 — Campaign cutover

`PlayableBattleService` no longer executes `DeterministicBattleEngine`.

Every campaign wave executes exactly one `RealtimeBattleRequest` through `RealtimeBattleExecutor`. Progression, reward and victory use `RealtimeBattleResult`. Campaign stars use elapsed duration rather than round count.

`RealtimeBattleCompatibilityAdapter` creates a legacy `BattleResult` projection from the same authoritative replay so older Unity builds can still function without a second simulation.

Temporary star gates are experimental only:

- 3 stars: <= 20,000 ms per wave;
- 2 stars: <= 40,000 ms per wave;
- 1 star: slower successful clear.

## M49 — Unity timestamp replay

Unity now models `timestampMs` / `durationMs`, promotes `realtimeBattle` into its existing presentation DTO and plays events according to simulation timestamp deltas.

`CAST_START` begins the animation. Equal-timestamp events execute without fabricated waits. `PlaybackSpeed` scales presentation only and does not change simulation results.

Legacy replay remains a fallback during staged migration.

## M50 — Competitive cutover

### Arena

`ArenaApplicationService` now executes one authoritative real-time battle.

- rating is resolved from `RealtimeBattleResult.outcome`;
- Arena Coin reward is resolved from the same outcome;
- audit persistence stores the real-time ruleset version and outcome;
- response includes both legacy `battle` projection and authoritative `realtimeBattle`.

### Shadow Arena

Every 5v5 squad in the best-of-three series now executes one real-time simulation.

- squad victory uses authoritative real-time outcome;
- DRAW tiebreak first compares authoritative `finalHp`;
- fallback tiebreak remains squad power then stable player seed order;
- persisted squad audit data stores `durationMs` and `rulesetVersion` instead of legacy round count;
- each squad response includes both compatibility `battle` and authoritative `realtimeBattle`.

`SERIES_RULES_VERSION` is now `bo3-hp-power-tiebreak-realtime-v2`.

### Unity competitive replay

`RealtimeBattleDtoCompatibility` now promotes Campaign, Arena and Shadow Arena. `PlayableGameStore` applies promotion immediately after each battle API response, so all three modes reuse the existing `BattleVisualStage` / `BattleTimelinePlayer` path.

## Current production-mode state

| Mode | Authoritative server combat | Unity presentation |
|---|---|---|
| Campaign / Adventure | Real-time | Timestamped replay |
| Arena | Real-time | Timestamped replay |
| Shadow Arena | Real-time per squad | Timestamped replay per squad |

No listed production battle mode needs the legacy engine for authoritative outcome/reward/rating after M50.

## Integrity gates

M47/M48 include Java unit coverage for deterministic replay, speed timelines, stable equal-time ordering, millisecond status lifecycle, compatibility projection and duration star thresholds.

M49/M50 add static migration gates:

- `scripts/validate-realtime-unity-replay.py`
- `scripts/validate-realtime-competitive-cutover.py`
- `.github/workflows/realtime-unity-replay-integrity.yml`
- `.github/workflows/realtime-competitive-cutover.yml`

The competitive gate explicitly rejects reintroduction of `DeterministicBattleEngine`, legacy `new BattleRequest(...)` or `BattleRuleset.experimentalV1()` in Arena/Shadow Arena application services.

## Remaining legacy debt

The migration is not complete while these contracts remain:

- `DeterministicBattleEngine` source and tests;
- `BattleRuleset.maxRounds`;
- `BattleEvent.round`;
- `BattleEvent.durationTurns`;
- `BattleResult.rounds`;
- `SkillEffectDefinition.durationTurns`;
- `PassiveTrigger.TURN_START` naming/semantics bridge;
- temporary legacy request bridge inside `RealtimeBattleExecutor`;
- `RealtimeBattleCompatibilityAdapter` and projected round/turn fields;
- UI copy that may still display projected rounds;
- production skill content that relies on default/converted timings.

## Known non-final timing values

The following remain engineering defaults until measured reference evidence exists:

- 50 ms simulation tick;
- speed/action-interval curve;
- default skill cooldowns;
- default cast/recovery times;
- temporary 3,000 ms legacy-turn conversion;
- default periodic status interval;
- 180 second battle timeout;
- campaign duration star thresholds.

These values must not be marked `VERIFIED` without evidence.

## Next milestones

### M51 — Content timing cutover

- author milliseconds for every production skill/status;
- remove production reliance on `durationTurns` conversion and default ability timing;
- add integrity checks requiring explicit real-time timing for production content;
- replace temporary turn-based passive terminology where appropriate.

### M52 — Legacy contract removal

- remove the request bridge from `RealtimeBattleExecutor`;
- make the real-time engine accept only `RealtimeBattleRequest`;
- remove old round engine once code search proves no production caller remains;
- remove projected `round`, `rounds`, `durationTurns` from server/client presentation paths;
- update UI status text to duration/time semantics.

### M53 — Runtime/device validation

- Unity Editor/play-mode replay tests;
- Android device replay/performance evidence;
- deterministic replay capture across server and Unity presentation;
- tune animation timing and speed controls without changing authoritative simulation.

## Release impact

M47–M50 substantially complete the architecture cutover for authoritative combat and replay, but they do not remove release blockers around production art, measured reference/parity evidence and Android device validation. Timing defaults remain experimental until those evidence gates are satisfied.
