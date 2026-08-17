# Real-Time Combat Migration Report

Milestone: M49 — continuous-time deterministic auto combat + Rage runtime.

## Architectural changes

- Authoritative global round/turn execution removed from the battle engine.
- Simulation advances by a deterministic priority queue of logical-time events.
- Stable ordering is `timestampMs -> event priority -> actor stable order -> sequence`.
- No actor threads, sleeps, Unity frame timing or system wall clock influence results.
- Battle timeout is `maxBattleDurationMs` with deterministic tie-breakers: living units, remaining HP ratio, damage dealt, then draw.

## Status/effect lifecycle

- `SkillEffectDefinition.durationTurns` replaced by `durationMs` and `tickIntervalMs`.
- Runtime status state uses apply/expiry/tick timestamps.
- STUN blocks action initiation and interrupts interruptible casts.
- SILENCE allows basics but blocks active/Rage skill decisions.
- BURN/POISON/BLEED tick by scheduled intervals.
- buffs/debuffs expire by logical simulation time.
- Speed modifiers reschedule future action readiness using the current experimental timing profile.

## Cooldown/cast/action architecture

- abilities define `cooldownMs`, `castTimeMs`, `recoveryMs`;
- actors track independent action readiness and action locks;
- cast completion is a scheduled event and can be invalidated by interruption;
- active skill cooldowns are independent instead of one fixed turn rotation.

## Rage / Nộ

- runtime Rage is clamped to 0..100;
- Basic attacks are the baseline universal Rage source;
- full Rage produces `RAGE_FULL` / `RAGE_SKILL_READY` events;
- Rage Skill is highest-priority eligible action when Rage is full;
- Rage Skill is not treated as a normal rotating cooldown action;
- exact gain/cost/frequency values remain EXPERIMENTAL pending reference evidence and M50 full Hero Version skill balance.

## Passive migration

- turn-start passives removed;
- periodic intent uses `TIME_INTERVAL` with explicit milliseconds;
- event hooks include battle/action/damage/HP/KO/status/skill/Rage-skill hooks;
- once-per-battle guard remains deterministic.

## Structured skill migration

`game-data/skills/technique-effects.csv` now uses `duration_ms,tick_interval_ms`. Persistent skills were reviewed individually rather than through a universal turn-to-second conversion. See `TIME_BASED_SKILL_AUDIT.md`.

## Unity/client

- battle DTO/events use `timestampMs`, `rageAfter`, `durationMs`;
- timeline playback waits on timestamp deltas;
- 1x/2x/3x alters wall-clock presentation only;
- pause stops presentation simulation-clock advancement;
- status duration labels count down in simulation seconds;
- Rage UI remains 0..100 and full Rage is visible.

## Tests / gates

M49 adds/updates coverage for:

- same seed/input -> identical timestamped events;
- Speed -> different independent action frequency;
- Rage cap/full/signature path;
- stable same-timestamp ordering;
- STUN expiry/action blocking;
- DOT tick interval exactness;
- event/time passive lifecycle;
- content schema rejection of deprecated turn duration fields;
- Unity static contract for timestamped playback.

The PR must pass Maven server tests, content integrity, database regressions, prior feature gates and `m49-realtime-combat-integrity` before merge.

## Unresolved balance/evidence questions

- exact Speed -> attack interval curve;
- exact universal/basic Rage gain and optional secondary Rage sources;
- per-Hero Version active cooldown/cast/recovery values;
- final Rage Skill coefficients and signatures;
- final per-mode battle duration limits;
- reference-specific control/buff durations.

These remain marked EXPERIMENTAL. M50 owns full Hero Version signature-skill research, differentiated 5/6-slot design, final real-time timing/balance, cinematics and EN/VI runtime descriptions. No parity claim is made without evidence.
