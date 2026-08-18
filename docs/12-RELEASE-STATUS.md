# Mobile Release Status

Current runtime checkpoint on `main`: **M53 — canonical deterministic realtime/Rage combat contracts.**  
Current completion-governance checkpoint on `main`: **M56 — authoritative completion roadmap and merge policy.**  
Current implementation candidate: **M54 / PR #75 — playable-quality battle presentation; implementation complete, CI/merge blocked until GitHub Actions actually allocates runners and required checks pass.**

Immediate integration queue:

1. **M54 / PR #75** — execute required CI on the exact head SHA, fix any real failures, then squash-merge into `main`.
2. **M55** — only after M54 merges, rebuild the Android Development APK + signed Release AAB pipeline from the newest `main`, validate CI, then merge.
3. **M57** — only after M55 merges, modernize realtime/Rage reference-evidence schemas and harden release gates.
4. Continue M58+ from the newest `main` following `docs/100-PERCENT-COMPLETION-PLAN.md`.

The old M17–M22 checkpoint text is no longer the source of truth.

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
| Reference/balance profiles VERIFIED | 0 / 10 | 10 / 10 (or 100% of final required profiles) | BLOCKED |
| Full production Campaign | vertical-slice content | 100% frozen stage census | BLOCKED |
| Resource PvE modes | foundations/partial | 100% frozen PvE census | BLOCKED |
| Arena/Shadow combat | realtime foundations | complete seasonal/meta/UI loop | PARTIAL |
| Battle presentation | M54 implementation candidate in PR #75 | production hero-specific presentation | IMPLEMENTED / CI BLOCKED |
| Production mobile UI screens | functional shell/partial | 100% release navigation graph | BLOCKED |
| Full new-account -> late-game E2E | not complete | PASS | BLOCKED |
| Passing Android device evidence | 0 | >=2 devices and >=2 classes | BLOCKED |
| Signed release AAB from canonical pipeline | not yet proven | PASS | BLOCKED |
| `release-audit.py --enforce` | not yet final-pass | PASS | BLOCKED |

`READY`, `VERIFIED` and `PARITY_PASS` are evidence states, not progress labels. TODO/CONCEPT/fallback assets and experimental balance values must never be counted as release-complete.

---

## What is already strong

### Architecture/runtime

- Java/Spring Boot server foundation with PostgreSQL/Redis contracts.
- Collectible Hero Version + one-time Awakening model.
- Deterministic continuous-time realtime combat.
- Rage 0–100 runtime and Rage Skill contract.
- Millisecond cooldown/cast/recovery/status timing.
- Timestamped deterministic event replay.
- Campaign/Arena/Shadow Arena authoritative realtime combat paths.
- Data-driven skill/effect/passive infrastructure.
- Player/account, wallet/energy and progression foundations.
- Summon/pity, inventory/equipment, shop, guild, daily/event, mail and localization foundations.

### Unity/mobile foundation

- Unity mobile project and generated navigation shell.
- Realtime replay consumption.
- Hero Version/Awakening presentation identity.
- Art/Addressables package contracts and development fallbacks.
- M54 candidate adds Pause/Resume, 1x/2x/4x playback, full pause-aware feedback, smooth HP/Rage presentation, visible fallback Rage meters, impact feedback, Rage Skill cinematic and replay lifecycle hardening.
- M55 Android build automation remains the next milestone but must not be normalized/merged until M54 lands.

### Validation foundation

- Server/core tests and deterministic combat tests.
- Content/reference validators.
- Art component/package/real-file gates.
- Android device evidence schema.
- Release audit infrastructure.
- M54 candidate adds playable-quality static validation plus Unity EditMode compile/regression coverage.

---

## Major release blockers

### 1. Current M54 CI/merge gate

PR #75 is ahead-only from `main`, but GitHub Actions jobs are currently failing before checkout with no allocated workflow steps. This is tracked in issue #76. M54 is not counted as merged or complete on `main` until required checks actually execute and pass on the exact head SHA.

### 2. Full skill review

The repository has complete structural coverage for Hero Versions/Awakenings, but structural coverage is not final design approval. The release requires complete identity/canon/editorial, mechanics/timing and balance review for all release skills, including presentation keys and deterministic regression coverage.

Planned: M58–M60.

### 3. Full production gameplay content

The existing code demonstrates the reusable systems, but the release still needs complete production Campaign, Resource PvE, competitive loops, progression tracks and coherent economy/live-loop content.

Planned: M61–M65.

### 4. Production mobile UI/UX

The current mobile shell/vertical slice is not the final game interface. Every release screen must receive production design, real interactions, mobile safe-area/aspect handling and loading/error/empty states.

Planned: M66–M68.

### 5. Production art/animation/VFX/audio

The release art gate requires concrete repository-backed files. Current tracked/READY counts are far below the 427-package release target.

Planned: M69–M73.

### 6. Reference/balance verification

Current balance profile file contains ten profiles and all ten are still `EXPERIMENTAL`. Realtime/Rage measurement schemas and corpora must be completed and every release-required profile must satisfy its evidence thresholds before `VERIFIED`.

Planned: M57 and M74.

### 7. Full automated acceptance/reliability

The project still needs one complete fresh-account -> late-game E2E journey plus persistence, migration, concurrency/idempotency, screenshot and Unity/device regression coverage.

Planned: M75.

### 8. Real Android release proof

Release requires real artifacts and physical-device evidence. GitHub Actions billing/runner availability, Unity licensing credentials and release signing secrets are external dependencies that cannot be replaced by fabricated evidence.

Planned: M55, M76 and M77.

---

## Reference/balance profile truth

`game-data/reference/balance-profiles.csv` currently defines ten release-relevant experimental profiles:

1. combat stats;
2. damage formula;
3. summon profile;
4. level cost;
5. ability cycle;
6. structured effects;
7. technique mapping;
8. passive lifecycle;
9. realtime timing;
10. Rage rules.

Current VERIFIED count: **0 / 10**.

M57 modernizes the measurement schemas and hardens production gates. M74 completes the measured verification/parity pass.

---

## Merge policy from now on

Read `docs/IMPLEMENTATION-MERGE-POLICY.md`.

The mandatory rule is:

```text
latest main
 -> one milestone branch
 -> implementation + validation
 -> PR to main
 -> required CI green on the exact head SHA
 -> merge
 -> next milestone from the new main
```

Do not repeat a long-lived stacked milestone chain. M55 must be normalized onto `main` after M54 lands.

---

## Completion roadmap

The detailed roadmap is `docs/100-PERCENT-COMPLETION-PLAN.md`.

High-level sequence:

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

---

## Desktop

Desktop remains gated behind successful mobile release certification. Do not split effort into a desktop product before the M77 mobile release-candidate gate passes. The desktop roadmap will reuse the Unity project, content, assets, deterministic battle runtime, Java backend and EN/VI localization after mobile is stable.
