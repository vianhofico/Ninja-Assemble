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

M54 removes the old `BattleVisualStage` damage-feedback subscription. Damage numbers, hit flashes and critical shake now have one owner: `BattleImpactFeedback`. Every feedback coroutine uses `TryPresentationDelta`: while paused it yields without changing alpha, position, scale or shake offset. This prevents the previous case where shake time stopped but the screen still randomized position every frame.

Feedback interruption is also explicit. `ResetShake` restores the stable stage origin, and a new shake first restores any interrupted shake before capturing its own origin. `ResetCinematic` stops the active cinematic and destroys its overlay before a replacement is started, preventing stacked letterbox overlays from surviving interrupted Rage casts.

`BattleVisualStage` remains responsible for actor/stage construction and victory presentation. Its fallback actor now exposes both HP and Rage meters through a shared meter builder, so Rage smoothing/readiness is testable even while the release art gate still routes most heroes through fallback presentation.

## Production asset path

The primitive runtime feedback is deliberately replaceable. Final hero-specific VFX can bind to existing `effectKey`, `abilityId`, `abilityKind`, `statusId` and actor anchors without changing server contracts.

## Validation

`scripts/validate-playable-quality.py` rejects loss of the 1x/2x/4x controls, full pause behavior, replay lifecycle hardening, interrupt-safe shake/cinematic cleanup, Rage cinematic hooks, unified damage feedback, fallback Rage visibility, canonical Rage UI naming, smooth HUD targets, Unity EditMode CI coverage or accidental reintroduction of the runtime `ULTIMATE` ability-kind branch.

`scripts/validate-battle-visual-stage.py` is migrated from the M22 `FloatingDamage`/`CriticalShake` contract to the M54 single-feedback architecture. It rejects duplicate stage feedback and the pre-Rage `ConfigureEnergyUi` method while requiring the fallback Rage meter.

`client-unity/Assets/Tests/Editor/BattlePlaybackContractTests.cs` exercises the playback-speed allowlist, reversible pause state, and empty-replay coroutine lifecycle under Unity EditMode.

`.github/workflows/playable-quality-integrity.yml` runs two mandatory jobs:

1. Python static/integrity validation, including the runtime visual-stage compatibility validator.
2. Unity EditMode compile/tests through `game-ci/unity-test-runner@v4` using repository Unity license secrets.

The Unity job is intentionally not optional: a source-only static pass is not sufficient proof that the C# presentation code compiles in the repository's Unity version.

## Merge gate

M54 may merge only when the exact PR head SHA has real runner execution and all required checks are green. A GitHub Actions job that fails before checkout with no steps is an infrastructure blocker, not a code PASS. Until that gate passes, `docs/12-RELEASE-STATUS.md` keeps M53 as the runtime checkpoint on `main` and records M54 only as an implementation candidate.

## Next

M55 creates the reproducible Android playtest build lane and artifacts. It must be rebuilt from the new `main` only after M54 merges. Real-device smoke/performance evidence remains a physical-device gate and cannot be fabricated by CI.
