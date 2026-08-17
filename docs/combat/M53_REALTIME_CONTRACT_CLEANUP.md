# M53 Realtime Contract Cleanup

## Outcome

M53 makes the post-M49 combat architecture explicit in code rather than carrying pre-realtime names as compatibility scaffolding.

Canonical server combat is now:

- `RealtimeBattleEngine`
- `RealtimeBattleRequest`
- `BattleRuleset` with millisecond duration/action timing
- `BattleEvent` with `timestampMs`, `durationMs` and `rageAfter`
- `BattleResult` with `durationMs` and authoritative `finalHp`

Campaign, Arena and Shadow Arena all invoke the same realtime engine contract directly.

## Removed contracts

The following pre-M53 compatibility contracts are removed:

- `BattleRequest.java`
- `DeterministicBattleEngine.java` legacy class name
- `DeterministicBattleEngineTest.java`
- runtime `ULTIMATE` ability-kind alias
- Java `energyDelta()` compatibility accessor
- Java `energyAfter()` compatibility accessor

The engine implementation remains deterministic continuous-time combat; M53 changes the contract/name and removes compatibility debt rather than introducing a second simulation implementation.

## Behavioral invariants preserved

- one deterministic logical event queue
- timestamp -> scheduled priority -> stable actor order -> sequence ordering
- independent speed-driven action timelines
- Rage 0..100 and signature Rage Skill behavior
- cooldown, cast and recovery timing in milliseconds
- timed STUN/SILENCE/DOT/status expiration
- passive lifecycle and interval passives
- timeout resolution and authoritative final HP
- same-seed replay determinism

## Validation

`validate-m53-realtime-contract.py` requires the canonical engine/request, all three production battle modes, realtime regression tests and removal of pre-Rage compatibility aliases.

`validate-legacy-combat-removal-readiness.py` is upgraded from a readiness report to an enforcement gate: legacy engine/request files must be absent and Java/Python source must not reference their symbols.

The M53 GitHub workflow also runs ability, passive and structured-effect validation.

## Next milestone

After M53 is green and merged, work moves to playable quality:

1. battle animation state machine polish
2. hit/critical/status VFX and screen feedback
3. Rage Skill cinematic presentation
4. battle HUD and playback controls
5. mobile interaction/game-feel pass
6. reproducible Android APK/AAB build and device smoke/performance evidence
