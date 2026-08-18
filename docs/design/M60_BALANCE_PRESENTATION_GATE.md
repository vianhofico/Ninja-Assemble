# M60 Balance and Presentation Review Gate

M60 adds static balance sanity plus evidence-backed full-roster balance/presentation approval.

## Static sanity

The audit rejects impossible structured-effect values: coefficient bounds, probability outside 0..10000 bps, invalid duration/tick windows and Rage deltas outside -100..100. It also requires every base skill to retain animation/VFX/SFX keys and every Rage Skill to retain `MINI_CINEMATIC`.

Static sanity is not balance approval.

## Review registry

`game-data/skills/m60-balance-presentation-reviews.csv` begins header-only. A `REVIEWED` record requires:

- a committed `simulation_ref` file inside the repository;
- an evidence reference;
- PvE/PvP and burst/sustain/control/survivability review scores;
- explicit counterplay and presentation review notes;
- presentation keys that still match the current production candidate.

This prevents stale simulation approvals surviving a presentation/content change.

## Production enforcement

`python scripts/validate-m60-balance-presentation.py --enforce` requires 970/970 base and 60/60 Awakening balance/presentation reviews. The production release workflow runs this after M58 identity and M59 mechanics enforcement.

## Truthful state

The registry is empty at integration. Development values remain experimental. No skill is promoted to balanced merely because its coefficient is within a sanity range.

## Next

M61 completes the Campaign production vertical-slice gate and census. M58–M60 review debt remains release-blocking while gameplay implementation proceeds.
