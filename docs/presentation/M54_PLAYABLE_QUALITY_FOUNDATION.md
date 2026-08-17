# M54 Playable Quality Foundation

M54 starts the production game-feel pass without coupling presentation to server simulation or requiring every final art asset to exist first.

## Added

- touch-friendly battle playback HUD with Pause/Resume and 1x/2x/4x controls
- actor Animator speed synchronization with replay speed and pause state
- smoother HP and Rage meter transitions
- asset-independent hit flashes and impact shake
- heal, shield, status, KO and Rage-ready feedback
- Rage Skill cinematic overlay / letterbox treatment
- compatibility fallback from the new `RageSkill` Animator trigger to existing `Ultimate` triggers so current prefabs do not break

## Architecture

`BattleTimelinePlayer` remains the owner of timestamp replay. It automatically attaches:

- `BattlePlaybackHud` for user controls
- `BattleImpactFeedback` for visual feedback

Neither module changes combat state, RNG, rewards or timestamps. They only consume presentation events.

## Production asset path

The primitive runtime feedback is deliberately replaceable. Final hero-specific VFX can bind to existing `effectKey`, `abilityId`, `abilityKind`, `statusId` and actor anchors without changing server contracts.

## Validation

`scripts/validate-playable-quality.py` rejects loss of the 1x/2x/4x controls, pause behavior, Rage cinematic hooks, impact feedback, smooth HUD targets or accidental reintroduction of the runtime `ULTIMATE` ability-kind branch.

## Next

M55 creates the reproducible Android playtest build lane and artifacts. Real-device smoke/performance evidence remains a physical-device gate and cannot be fabricated by CI.
