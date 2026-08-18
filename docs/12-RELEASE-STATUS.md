# Mobile Release Status

Current implementation checkpoint: **M55 — reproducible Android build pipeline foundation.**  
Current completion-governance checkpoint: **M56 — authoritative completion roadmap and merge policy.**

M54 is merged in `main`. M55 adds the canonical Android Development APK lane, signed Release AAB lane, Unity build automation, version/signing metadata contract and Android build validator. GitHub Actions runner allocation is still externally unavailable, so M55 integration records **pipeline implemented / artifact execution unverified** rather than fabricating an APK/AAB PASS.

Immediate integration queue:

1. **M57** — modernize realtime/Rage reference-evidence schemas and harden production release gates.
2. **M58–M60** — complete hero skill identity/editorial, mechanics/timing and balance/presentation review.
3. Continue M61+ from the newest `main` following `docs/100-PERCENT-COMPLETION-PLAN.md`.

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
| Battle presentation | M54 foundation integrated | production hero-specific presentation | FOUNDATION PASS |
| Android build pipeline | M55 pipeline integrated | reproducible APK + signed AAB evidence | IMPLEMENTED / EXECUTION UNVERIFIED |
| Passing Android device evidence | 0 | >=2 devices and >=2 classes | BLOCKED |
| Full production Campaign | vertical-slice content | 100% frozen stage census | BLOCKED |
| Resource PvE modes | foundations/partial | 100% frozen PvE census | BLOCKED |
| Arena/Shadow combat | realtime foundations | complete seasonal/meta/UI loop | PARTIAL |
| Production mobile UI screens | functional shell/partial | 100% release navigation graph | BLOCKED |
| Full new-account -> late-game E2E | not complete | PASS | BLOCKED |
| Signed release AAB from canonical pipeline | workflow defined; not executed | PASS | BLOCKED |
| `release-audit.py --enforce` | not yet final-pass | PASS | BLOCKED |

`READY`, `VERIFIED`, `PARITY_PASS`, artifact PASS and device PASS are evidence states, not progress labels. Missing runners/secrets/hardware must never be converted into fake evidence.

---

## M54 integrated scope

- Pause/Resume + 1x/2x/4x replay controls.
- Smooth, pause-aware HP/Rage presentation.
- Unified impact/feedback path and interrupt-safe Rage cinematic.
- Fallback HP + Rage meters.
- Replay lifecycle hardening and canonical Rage presentation naming.

## M55 integrated scope

- `game-ci/unity-builder@v5` Android workflow.
- PR Development APK lane.
- Manual signed Release AAB lane.
- Unity 6000 project/version auto-detection.
- IL2CPP + ARM64 + landscape player settings.
- version/versionCode command-line/environment handling.
- isolated keystore/signing configuration; no secret material committed.
- build metadata with commit/version/output/size/duration/scene information.
- Android contract validator and physical-device evidence separation.

M55 does **not** claim that an APK/AAB has been produced while GitHub Actions and Unity credentials are unavailable. Actual artifact generation remains to be demonstrated when execution is restored; release certification stays gated at M76/M77.

---

## Major release blockers

### Reference/balance evidence — M57 + M74

All ten release-relevant profiles remain `EXPERIMENTAL`; VERIFIED count is **0 / 10**. Realtime/Rage schema/corpus hardening starts at M57.

### Full skill completion — M58–M60

Structural Hero Version/Awakening coverage exists, but final identity/editorial, deterministic mechanics/timing, balance and presentation review remain incomplete.

### Full production gameplay — M61–M65

Campaign, Resource PvE, competitive loops, progression tracks and economy/live loops still need complete production vertical slices.

### Production mobile UI — M66–M68

The functional shell is not the release UI. Production UX/state handling/safe-area/aspect support remain incomplete.

### Production art — M69–M73

Real repository-backed art/animation/VFX/audio READY coverage remains **0 / 427**.

### E2E/reliability — M75

Fresh-account -> late-game E2E, persistence, migrations, concurrency/idempotency and regression evidence remain incomplete.

### Android/device/release proof — M76–M77

Real artifacts, physical-device evidence, signing proof and final release audit remain hard evidence gates. The CI-outage exception cannot fabricate these results.

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

---

## Roadmap

```text
M54 battle presentation [MERGED]
 -> M55 Android build lane [INTEGRATION]
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
