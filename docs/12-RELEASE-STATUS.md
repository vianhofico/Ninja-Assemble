# Mobile Release Status

Current runtime checkpoint: **M54 — playable-quality battle presentation foundation.**  
Current completion-governance checkpoint: **M56 — authoritative completion roadmap and merge policy.**

M54 was rebuilt cleanly from the latest `main` as `agent/m54-reintegrate-v3` after GitHub Actions repeatedly failed before runner allocation (`steps=null`). CI was checked and the infrastructure failure was documented; because M54 is a non-release presentation milestone, merge proceeds under the CI-outage exception in `docs/IMPLEMENTATION-MERGE-POLICY.md` after final diff/source-validator audit. This does **not** claim Unity CI passed.

Immediate integration queue:

1. **M55** — rebuild the Android Development APK + signed Release AAB pipeline from the newest `main`, validate the build contract, then merge.
2. **M57** — modernize realtime/Rage reference-evidence schemas and harden production release gates.
3. Continue M58+ from the newest `main` following `docs/100-PERCENT-COMPLETION-PLAN.md`.

---

## Current completion dashboard

| Area / gate | Current | Release target | State |
|---|---:|---:|---|
| Base/reference characters | 189 | >=180 | PASS foundation |
| Collectible Hero Versions | 194 | frozen release roster | PASS structure |
| Base skill slots | 970 (=194×5) | 100% final reviewed | STRUCTURE PASS / REVIEW BLOCKED |
| Awakening Skills | 60 | 100% final reviewed | STRUCTURE PASS / REVIEW BLOCKED |
| Playable/reference variant census | 427 | frozen release presentation census | PASS census |
| Production art packages tracked | 12 / 427 | 427 / 427 | BLOCKED |
| Production art packages fully READY | 0 / 427 | 427 / 427 | BLOCKED |
| Reference/balance profiles VERIFIED | 0 / 10 | 10 / 10 | BLOCKED |
| Full production Campaign | vertical-slice content | 100% frozen stage census | BLOCKED |
| Resource PvE modes | foundations/partial | 100% frozen PvE census | BLOCKED |
| Arena/Shadow combat | realtime foundations | complete seasonal/meta/UI loop | PARTIAL |
| Battle presentation | M54 foundation integrated | production hero-specific presentation | FOUNDATION PASS |
| Production mobile UI screens | functional shell/partial | 100% release navigation graph | BLOCKED |
| Full new-account -> late-game E2E | not complete | PASS | BLOCKED |
| Passing Android device evidence | 0 | >=2 devices and >=2 classes | BLOCKED |
| Signed release AAB from canonical pipeline | not yet proven | PASS | BLOCKED |
| `release-audit.py --enforce` | not yet final-pass | PASS | BLOCKED |

`READY`, `VERIFIED` and `PARITY_PASS` are evidence states, not progress labels. TODO/CONCEPT/fallback assets and experimental balance values must never be counted as release-complete.

---

## M54 integrated scope

- Pause/Resume and 1x/2x/4x replay controls.
- Smooth HP/Rage presentation with pause-aware interpolation.
- Animator/audio presentation-rate synchronization.
- Single pause-aware damage/impact feedback path.
- Heal/shield/status/KO/Rage-ready feedback.
- Interrupt-safe Rage Skill cinematic and shake handling.
- Fallback actors expose HP + Rage meters.
- Empty/all-zero replay lifecycle hardening.
- Canonical Rage presentation naming.
- Playable-quality static validator and Unity EditMode regression gate definitions.

GitHub Actions runner allocation remains an external infrastructure issue. M54 merge does not fabricate CI execution; the missing Unity runner result remains documented and must be rechecked when Actions becomes available.

---

## Major release blockers

### Full skill review — M58–M60

Structural Hero Version/Awakening coverage exists, but final identity/editorial, mechanics/timing, balance, presentation keys and deterministic regression review are incomplete.

### Full production gameplay — M61–M65

Campaign, Resource PvE, competitive loops, progression tracks and economy/live loops need complete production vertical slices.

### Production mobile UI — M66–M68

The current shell is not the final game interface. Release screens need production UX, state handling, safe areas/aspect support and complete interactions.

### Production art — M69–M73

Release requires concrete repository-backed art/animation/VFX/audio packages. Current READY coverage is 0 / 427.

### Reference/balance verification — M57 + M74

All ten release-relevant balance/reference profiles remain `EXPERIMENTAL`; current VERIFIED count is **0 / 10**.

### E2E/reliability — M75

Fresh-account -> late-game E2E, persistence, migrations, concurrency/idempotency and regression evidence remain incomplete.

### Android release proof — M55 + M76 + M77

Real Android artifacts, signing, physical-device evidence and final release audit remain required. External secrets/hardware evidence must not be fabricated.

---

## Merge workflow

```text
latest main
 -> one milestone branch
 -> implementation + validators/tests available
 -> final diff review
 -> check CI
 -> if CI runs: fix real failures
 -> if non-release CI cannot allocate a runner: document outage and use policy §6 exception when equivalent validation is genuine
 -> squash merge
 -> next milestone from new main
```

Release-certification milestones M76/M77 keep hard evidence gates and may not use the CI-outage exception to fabricate build/device/release success.

---

## Roadmap

```text
M54 battle presentation
 -> M55 Android build lane
 -> M57 evidence schema/gates
 -> M58-M60 full skill completion
 -> M61-M65 full gameplay/content loops
 -> M66-M68 production mobile UI
 -> M69-M73 full production art
 -> M74 parity/balance verification
 -> M75 E2E/reliability
 -> M76 real-device certification
 -> M77 production hardening/release candidate
```

Desktop remains gated behind the M77 mobile release-candidate gate.
