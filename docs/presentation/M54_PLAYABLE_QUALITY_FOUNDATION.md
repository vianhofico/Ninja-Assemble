# M54 Playable Quality Foundation

M54 starts the production game-feel pass without coupling presentation to server simulation or requiring every final art asset to exist first.

## Added

- touch-friendly battle playback HUD with Pause/Resume and 1x/2x/4x controls
- actor Animator/audio presentation-rate synchronization with replay speed and pause state
- full pause freeze for HP/Rage interpolation, animation/audio, status clocks and feedback mutations
- smoother HP and Rage meter transitions while playback is running
- visible Rage meters on fallback actors so the current vertical slice exposes Rage progression before production prefabs are READY
- asset-independent hit flashes, damage numbers and impact shake
- heal, shield, status, KO and Rage-ready feedback
- Rage Skill cinematic overlay / letterbox treatment
- interruption-safe feedback: replacement shakes restore the stable stage origin and replacement Rage cinematics destroy the previous overlay
- compatibility fallback from the new `RageSkill` Animator trigger to existing `Ultimate` triggers so current prefabs do not break
- canonical `ConfigureRageUi` presentation API while preserving only the serialized `energySlider` migration attribute for old prefabs
- replay lifecycle hardening so empty/all-zero-timestamp event streams cannot leave a stale `IsPlaying` coroutine handle

## Architecture

`BattleTimelinePlayer` remains the owner of timestamp replay. It automatically attaches:

- `BattlePlaybackHud` for user controls
- `BattleImpactFeedback` for visual feedback

Neither module changes combat state, RNG, rewards or timestamps. They only consume presentation events. `BattleActorView` owns visual interpolation and presentation rate; pause freezes presentation without changing the authoritative replay timeline.

`BattleTimelinePlayer` deliberately suspends once before consuming the first replay event. This guarantees `Play()` stores the coroutine handle before even an empty or zero-delay replay can complete and clear that handle. Null replay entries are ignored, and a replay paused before an event waits until resume.

M54 removes the old `BattleVisualStage` damage-feedback subscription. Damage numbers, hit flashes and critical shake now have one owner: `BattleImpactFeedback`. Every feedback coroutine uses `TryPresentationDelta`: while paused it yields without changing alpha, position, scale or shake offset.

Feedback interruption is explicit. `ResetShake` restores the stable stage origin, and a new shake first restores any interrupted shake before capturing its own origin. `ResetCinematic` stops the active cinematic and destroys its overlay before a replacement is started.

`BattleVisualStage` remains responsible for actor/stage construction and victory presentation. Its fallback actor exposes both HP and Rage meters through a shared meter builder.

## Validation

`scripts/validate-playable-quality.py` rejects loss of the 1x/2x/4x controls, full pause behavior, replay lifecycle hardening, interrupt-safe shake/cinematic cleanup, Rage cinematic hooks, unified damage feedback, fallback Rage visibility, canonical Rage UI naming, smooth HUD targets, Unity EditMode CI coverage or accidental reintroduction of the runtime `ULTIMATE` ability-kind branch.

`scripts/validate-battle-visual-stage.py` is migrated from the M22 `FloatingDamage`/`CriticalShake` contract to the M54 single-feedback architecture. It rejects duplicate stage feedback and the pre-Rage `ConfigureEnergyUi` method while requiring the fallback Rage meter.

`client-unity/Assets/Tests/Editor/BattlePlaybackContractTests.cs` covers the playback-speed allowlist, reversible pause state and empty-replay lifecycle under Unity EditMode.

`.github/workflows/playable-quality-integrity.yml` defines Python static/runtime-stage validation plus Unity EditMode compile/tests through `game-ci/unity-test-runner@v4`.

## CI outage handling

GitHub Actions was checked repeatedly on the exact M54 head, but hosted jobs failed before runner allocation with `steps=null`; checkout, Python and Unity steps never executed. This is recorded as an infrastructure failure, not as a passing test result.

M54 is a non-release presentation milestone, so the reintegration branch may merge under section 6 of `docs/IMPLEMENTATION-MERGE-POLICY.md` after:

1. rebuilding from the latest `main`;
2. confirming an ahead-only final diff;
3. source/validator contract review;
4. documenting the unavailable CI execution.

This exception does **not** apply to release certification. Unity compile remains unverified until a runner and Unity credentials are available, and M76/M77 still require real build/device/release evidence.

## Next

M55 creates the reproducible Android playtest build lane and artifacts from the new `main`. Real-device smoke/performance evidence remains a physical-device gate and cannot be fabricated by CI or by the outage exception.
