# M54 Playable Quality Foundation

M54 starts the production game-feel pass without coupling presentation to server simulation or requiring every final art asset to exist first.

## Added

- touch-friendly battle playback HUD with Pause/Resume and 1x/2x/4x controls
- actor Animator/audio presentation-rate synchronization with replay speed and pause state
- full pause freeze for HP/Rage interpolation as well as animation/audio/status clock presentation
- smoother HP and Rage meter transitions while playback is running
- asset-independent hit flashes, damage numbers and impact shake
- heal, shield, status, KO and Rage-ready feedback
- Rage Skill cinematic overlay / letterbox treatment
- compatibility fallback from the new `RageSkill` Animator trigger to existing `Ultimate` triggers so current prefabs do not break
- replay lifecycle hardening so empty/all-zero-timestamp event streams cannot leave a stale `IsPlaying` coroutine handle

## Architecture

`BattleTimelinePlayer` remains the owner of timestamp replay. It automatically attaches:

- `BattlePlaybackHud` for user controls
- `BattleImpactFeedback` for visual feedback

Neither module changes combat state, RNG, rewards or timestamps. They only consume presentation events. `BattleActorView` owns visual interpolation and presentation rate; pause freezes presentation without changing the authoritative replay timeline.

`BattleTimelinePlayer` deliberately suspends once before consuming the first replay event. This guarantees `Play()` stores the coroutine handle before even an empty or zero-delay replay can complete and clear that handle. Null replay entries are ignored, and a replay already paused before its first event waits until resume.

M54 also removes the old `BattleVisualStage` damage-feedback subscription. Damage numbers, hit flashes and critical shake now have one owner: `BattleImpactFeedback`. This avoids double critical shake and prevents a legacy `Time.deltaTime` coroutine from continuing while the custom replay clock is paused. `BattleVisualStage` remains responsible for actor/stage construction and victory presentation only.

## Production asset path

The primitive runtime feedback is deliberately replaceable. Final hero-specific VFX can bind to existing `effectKey`, `abilityId`, `abilityKind`, `statusId` and actor anchors without changing server contracts.

## Validation

`scripts/validate-playable-quality.py` rejects loss of the 1x/2x/4x controls, full pause behavior, replay lifecycle hardening, Rage cinematic hooks, unified damage feedback, smooth HUD targets, Unity EditMode CI coverage or accidental reintroduction of the runtime `ULTIMATE` ability-kind branch.

`scripts/validate-battle-visual-stage.py` is migrated from the M22 `FloatingDamage`/`CriticalShake` contract to the M54 single-feedback architecture and rejects reintroduction of the duplicate stage subscription.

`client-unity/Assets/Tests/Editor/BattlePlaybackContractTests.cs` exercises the playback-speed allowlist, reversible pause state, and empty-replay coroutine lifecycle under Unity EditMode.

`.github/workflows/playable-quality-integrity.yml` runs two mandatory jobs:

1. Python static/integrity validation, including the runtime visual-stage compatibility validator.
2. Unity EditMode compile/tests through `game-ci/unity-test-runner@v4` using repository Unity license secrets.

The Unity job is intentionally not optional: a source-only static pass is not sufficient proof that the C# presentation code compiles in the repository's Unity version.

## Merge gate

M54 may merge only when the exact PR head SHA has real runner execution and all required checks are green. A GitHub Actions job that fails before checkout with no steps is an infrastructure blocker, not a code PASS.

## Next

M55 creates the reproducible Android playtest build lane and artifacts. Real-device smoke/performance evidence remains a physical-device gate and cannot be fabricated by CI.
