# Mobile Release Status

Current runtime checkpoint: **M53 — canonical deterministic realtime/Rage combat contracts on `main`.**  
Current completion-governance checkpoint: **M56 — authoritative completion roadmap and merge policy.**

Immediate integration queue:

1. **M54 / PR #72** — playable-quality battle HUD, impact feedback and Rage Skill presentation.
2. **M55 / PR #73** — Android Development APK + signed Release AAB pipeline. M55 currently depends on M54 and must be refreshed against `main` after M54 merges.
3. After M54/M55 integration, continue M57+ from the newest `main` following `docs/100-PERCENT-COMPLETION-PLAN.md`.

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
- M54 battle presentation work prepared in PR #72.
- M55 Android build automation work prepared in PR #73.

### Validation foundation

- Server/core tests and deterministic combat tests.
- Content/reference validators.
- Art component/package/real-file gates.
- Android device evidence schema.
- Release audit infrastructure.

---

## Major release blockers

### 1. Full skill review

The repository has complete structural coverage for Hero Versions/Awakenings, but structural coverage is not final design approval. The release requires complete identity/canon/editorial, mechanics/timing and balance review for all release skills, including presentation keys and deterministic regression coverage.

Planned: M58–M60.

### 2. Full production gameplay content

The existing code demonstrates the reusable systems, but the release still needs complete production Campaign, Resource PvE, competitive loops, progression tracks and coherent economy/live-loop content.

Planned: M61–M65.

### 3. Production mobile UI/UX

The current mobile shell/vertical slice is not the final game interface. Every release screen must receive production design, real interactions, mobile safe-area/aspect handling and loading/error/empty states.

Planned: M66–M68.

### 4. Production art/animation/VFX/audio

The release art gate requires concrete repository-backed files. Current tracked/READY counts are far below the 427-package release target.

Planned: M69–M73.

### 5. Reference/balance verification

Current balance profile file contains ten profiles and all ten are still `EXPERIMENTAL`. Realtime/Rage measurement schemas and corpora must be completed and every release-required profile must satisfy its evidence thresholds before `VERIFIED`.

Planned: M57 and M74.

### 6. Full automated acceptance/reliability

The project still needs one complete fresh-account -> late-game E2E journey plus persistence, migration, concurrency/idempotency, screenshot and Unity/device regression coverage.

Planned: M75.

### 7. Real Android release proof

CI code is being prepared, but release requires real artifacts and physical-device evidence. GitHub Actions billing/runner availability, Unity licensing credentials and release signing secrets are external dependencies that cannot be replaced by fabricated evidence.

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
 -> merge
 -> next milestone from the new main
```

Do not repeat a long-lived stacked milestone chain. Current M54 -> M55 is the last accepted temporary dependency and must be normalized after M54 merges.

---

## Completion roadmap

The detailed roadmap is `docs/100-PERCENT-COMPLETION-PLAN.md`.

High-level sequence:

```text
M54 battle presentation
 -> M55 Android build lane
 -> M56 completion baseline
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
