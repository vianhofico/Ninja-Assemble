# Ninja Assemble — 100% Completion Implementation Plan

Status: **authoritative execution roadmap after M53**  
Target platform: **Android mobile first**  
Desktop: starts only after the mobile release gate passes  
Architecture baseline: Unity client + Java/Spring Boot server + PostgreSQL + Redis + deterministic realtime combat

This document turns the repository audit into an implementation sequence that can be executed milestone by milestone. It complements `docs/00-MASTER-PLAN.md`: the Master Plan defines the product/parity target; this document defines the path from the current M53+ codebase to a production-complete mobile release.

---

## 1. Definition of “100% complete”

The project is complete only when all of the following are true at the same commit on `main`:

1. All planned mobile gameplay systems are implemented end to end: data -> server/domain -> persistence -> API -> Unity -> localization -> tests.
2. All release-scope Hero Versions and Awakenings have final reviewed skill kits, not unresolved research placeholders.
3. All release-scope campaign, PvE, PvP, progression, economy, guild, quest and event loops are playable from a fresh account.
4. Every release-scope screen has production UI/UX, loading/error/empty states and mobile safe-area behavior.
5. Every release-scope hero presentation package has real repository-backed portrait/icon/chibi/animation/VFX/SFX/regression assets and is marked `READY` by the art gates.
6. Every required reference/balance profile is evidence-backed and `VERIFIED`; release features reach `PARITY_PASS` where observable-parity is the target.
7. Deterministic/golden combat, economy, persistence, migration, E2E, Unity and device tests pass.
8. A reproducible Android build exists, a signed release AAB can be produced, and physical-device smoke/performance evidence passes on at least two distinct Android device models across at least two performance classes.
9. `release-audit --enforce` and all production gates pass without bypasses, fake evidence or TODO/CONCEPT rows being counted as complete.
10. Release documentation matches the exact `main` commit being shipped.

A feature is not complete because a class, endpoint or placeholder screen exists. It is complete only after its own Definition of Done and release gates pass.

---

## 2. Current baseline

### Already integrated on `main`

The codebase already contains substantial foundations:

- account/player state, wallet and energy;
- collectible Hero Version + one-time Awakening model;
- deterministic continuous-time Rage combat;
- millisecond cooldown/cast/recovery/status timing;
- timestamped replay contracts;
- Campaign/Arena/Shadow Arena realtime combat integration;
- hero progression and Frame Advance foundations;
- summon/pity, inventory/equipment, shops, guild, daily/events, mail and localization foundations;
- Unity mobile shell and playable vertical slice;
- content/reference/art/release validation infrastructure.

M53 is the canonical realtime combat cleanup baseline.

### Open integration work immediately ahead

- **M54 / PR #72** — playable-quality battle presentation foundation.
- **M55 / PR #73** — Android playtest APK + signed AAB pipeline; currently stacked on M54.

### Major unresolved release blockers

- 427 release-scope variant presentation packages are not production-complete.
- Reference/balance profiles are still experimental rather than fully verified.
- Hero skill structure is complete, but final canon/effect/timing/balance/editorial review is not complete for the entire release roster.
- Campaign and several game modes have foundation/vertical-slice content rather than full production content.
- Most Unity screens are functional shells rather than final production UI.
- Physical Android device evidence is not complete.
- GitHub Actions/Unity licensing/billing configuration can block automated Unity builds and must be treated as an external release dependency, not hidden by code changes.

---

## 3. Mandatory delivery policy

Every milestone after this plan follows the same rule:

> **Start from the latest `main`, finish one milestone, validate it, merge it into `main`, then start the next milestone from the new `main`.**

Do not create long-lived stacked milestone chains. A temporary stacked PR is allowed only when a strict dependency makes it unavoidable; once the parent merges, the child must be rebased/rebuilt/retargeted so its final diff against `main` contains only that milestone.

The detailed Git/PR policy is in `docs/IMPLEMENTATION-MERGE-POLICY.md`.

---

# 4. Execution roadmap

## Phase A — Integration and execution baseline

### M54 — Playable-quality battle presentation

**Objective**

Merge the existing clean M54 battle presentation work into `main` without changing authoritative combat semantics.

**Scope**

- Pause/Resume.
- Replay speeds 1x/2x/4x.
- Current simulation timestamp/action display.
- Smooth HP/Rage interpolation.
- Hit flash/impact shake.
- Heal/shield/status/KO feedback.
- Rage-ready feedback.
- Rage Skill cinematic overlay.
- Animator/audio presentation speed synchronization.

**Required validation**

- Static playable-quality validator.
- Existing realtime replay validators.
- Unity compilation when runner/license is available.
- Manual smoke in Unity Editor before calling the visual UX final.

**Definition of Done**

- PR #72 diff contains only M54 scope.
- No server combat result/RNG/reward behavior changes.
- Required checks pass or any external runner blocker is explicitly documented.
- M54 is merged into `main`.

---

### M55 — Reproducible Android build lane

**Dependency:** M54 merged.

**Objective**

Make Android development/release artifacts reproducible and auditable.

**Scope**

- Development APK workflow.
- Signed release AAB workflow.
- Unity project version detection.
- IL2CPP + ARM64 production settings.
- Version/versionCode/build metadata.
- Ephemeral keystore handling.
- Artifact upload/retention.
- Build validation script.

**Integration rule**

PR #73 is currently based on M54. After M54 merges, ensure the M55 branch/diff is clean against latest `main`; do not merge a stale stacked snapshot.

**External prerequisites**

- GitHub Actions billing/spending state fixed.
- `UNITY_LICENSE`, `UNITY_EMAIL`, `UNITY_PASSWORD` configured.
- Android signing secrets configured for release AAB.

**Definition of Done**

- Development APK is produced by the canonical build pipeline when runner credentials are available.
- Release AAB lane rejects missing signing input and produces a signed AAB when credentials are present.
- Metadata identifies git SHA, Unity version, artifact type and versionCode.
- M55 is merged into `main`.

---

### M56 — Completion baseline, documentation and strict merge discipline

**Objective**

Make the completion roadmap itself authoritative and prevent the repository from drifting back into stale status reporting or stacked milestone chains.

**Scope**

- Add this 100% completion plan.
- Add implementation/merge policy.
- Rewrite `docs/12-RELEASE-STATUS.md` to M53+ truth.
- Link future PRs to one milestone and one explicit Definition of Done.
- Record external blockers separately from code blockers.
- Close/retire superseded PRs after confirming their behavior is already integrated.

**Definition of Done**

- Current status docs agree with `main` and open PR state.
- Every future milestone has explicit entry/exit criteria.
- No new milestone starts from an older feature branch when latest `main` is available.
- M56 documentation PR is merged into `main`.

---

## Phase B — Evidence model and content correctness

### M57 — Reference schema modernization and evidence gate hardening

**Objective**

Align all research/measurement schemas with canonical realtime/Rage combat and make parity evidence a real production gate.

**Tasks**

- Audit every file under `game-data/reference/measurements`.
- Replace stale `duration_turns` fields with `duration_ms` + `tick_interval_ms`.
- Replace stale energy terminology with Rage terminology where the runtime contract has changed.
- Add/finish realtime timing measurement corpus.
- Add/finish Rage rule measurement corpus.
- Make `validate-reference-evidence.py` understand every release profile.
- Add `validate-reference-evidence.py` to production release workflow.
- Add `release-audit.py --enforce` to production release workflow.
- Prevent `EXPERIMENTAL`, `UNKNOWN`, `INFERRED`, `RESEARCH_REQUIRED` values from being promoted to verified states without evidence.

**DoD**

- No release measurement schema is turn/round based.
- Every required balance profile has a defined sample threshold and evidence format.
- Production CI fails if required reference evidence is missing or falsely promoted.
- M57 merged into `main`.

---

### M58 — Hero skill audit batch 1: identity/canon/editorial

**Objective**

Review the complete release Hero Version catalog for skill identity before numerical balance tuning.

**Tasks for every Hero Version**

- Confirm five base slots: BASIC / SKILL_1 Rage Skill / SKILL_2 / SKILL_3 / PASSIVE.
- Confirm at most one Awakening and exactly one separate Awakening Skill when applicable.
- Review EN/VI names and descriptions.
- Review signature technique identity and role fantasy.
- Resolve duplicate full kits between collectible versions.
- Resolve `RESEARCH_REQUIRED` canon/source-confidence debt.
- Record evidence/source metadata.

**DoD**

- No release Hero Version is missing one of the five normal slots.
- No release Awakening is missing its sixth skill.
- No unresolved identity conflict remains.
- Editorial/canon status is final for the full release roster.
- M58 merged into `main`.

---

### M59 — Hero skill audit batch 2: mechanics and realtime timing

**Objective**

Turn every final skill identity into an explicit executable realtime mechanic.

**Tasks**

For all release skills:

- target selector;
- damage/heal/shield coefficients;
- Rage cost/gain;
- cooldownMs;
- castTimeMs;
- recoveryMs;
- status type/chance/durationMs/tickIntervalMs;
- cleanse/dispel/revive/control semantics;
- passive trigger and internal cooldown where applicable;
- ability/effect/VFX/animation keys;
- deterministic ordering expectations.

**DoD**

- No production skill relies on hidden default timing.
- No production status relies on turn duration.
- All skills parse/resolve through data-driven definitions rather than hero-specific battle-engine branches.
- Representative deterministic tests cover each mechanic family.
- M59 merged into `main`.

---

### M60 — Hero skill audit batch 3: balance, simulations and presentation contract

**Objective**

Make the complete skill catalog playable and internally balanced before multiplying game content.

**Tasks**

- Batch deterministic simulation across rarity/role/team archetypes.
- Detect impossible loops, permanent control, runaway shields/heals, zero-counterplay Rage loops and dead skills.
- Establish release balance budgets/ranges.
- Validate each skill has presentation keys.
- Validate every Rage Skill/Awakening Skill has a cinematic/presentation specification even if final art arrives later.
- Freeze a versioned skill ruleset.

**DoD**

- No release skill remains `RESEARCH_REQUIRED` or `UNREVIEWED`.
- Balance status is explicitly final or evidence-gated with no hidden placeholder values.
- Simulation regressions are stored as golden expectations where appropriate.
- M60 merged into `main`.

---

## Phase C — Full playable game content

### M61 — Main Quest production expansion

**Objective**

Replace the small campaign vertical slice with production progression.

**Tasks**

- Freeze chapter/difficulty census from reference evidence.
- Normal progression.
- Elite progression.
- Heroic progression only if confirmed in target scope.
- Enemy/wave formations.
- Boss stages.
- First-clear/repeat rewards.
- Star thresholds and star milestone rewards.
- Stamina costs.
- Unlock requirements.
- Sweep/replay rules after verification.
- Player progression curve.
- Unity chapter/stage map and result flows.

**DoD**

- A fresh account can progress through the complete release-scope Main Quest.
- Every stage has valid enemies, rewards and unlock rules.
- No release stage is a placeholder row.
- Campaign progression/reward/E2E tests pass.
- M61 merged into `main`.

---

### M62 — Resource PvE production completion

**Objective**

Implement every release-scope PvE mode end to end, not just domain definitions.

**Target systems**

- Ninja Trial variants.
- Forest Hunt variants.
- Resource Raid/Challenge modes confirmed by census.
- Battle Relief if confirmed.
- Obito Ultimate Trial if confirmed.
- Tailed Beast Conquer.
- Crusade.
- Ninja Quest and other confirmed release modes.

**For each mode**

- rules/restrictions;
- schedule/reset/attempt count;
- stage/enemy data;
- cost;
- reward table;
- application service;
- persistence;
- API;
- Unity entry/battle/result UX;
- EN/VI localization;
- tests and parity evidence.

**DoD**

Every release-scope PvE census row is either `PARITY_PASS` or explicitly removed from target scope with documented evidence/reason. M62 merged into `main`.

---

### M63 — PvP/competitive production completion

**Objective**

Turn Arena and Shadow Arena combat foundations into complete competitive loops.

**Arena**

- matchmaking/opponent snapshot rules;
- defense formation;
- rating/rank progression;
- attempts/costs;
- daily/season rewards;
- history;
- ranking UI;
- Arena store/currency integration.

**Shadow Arena**

- 15-ninja roster validation;
- three squads;
- best-of-three resolution;
- draw/tiebreak rules;
- ranking/season rewards;
- history;
- Shadow currency/store integration.

**DoD**

- Rank/reward transactions are idempotent and exploit-resistant.
- Full competitive UI flows exist.
- Season/reset behavior is tested.
- M63 merged into `main`.

---

### M64 — Progression systems completion

**Objective**

Finish long-term character/account progression systems.

**Tracks**

- Hero level/EXP.
- Frame Advance.
- Skill progression if release-scope reference confirms it.
- Equipment progression.
- Scroll/Ninja College progression.
- Tailed Beast/Jinchuriki progression.
- Awakening prerequisites/costs.
- Team synergy/bonds.

**Special requirement**

The Scroll subsystem must be explicitly audited. If current generic progression code does not represent the release-scope Scroll/Ninja College behavior, add dedicated data/domain/application/API/UI paths rather than claiming the system complete from unrelated foundations.

**DoD**

- Every progression track has costs, caps, persistence, server validation, Unity UX and tests.
- No progression currency can become negative or double-spent under retry/concurrency.
- M64 merged into `main`.

---

### M65 — Economy, inventory, shops, summon, guild, quest and event completion

**Objective**

Replace sample economy content with a coherent production economy.

**Tasks**

- Complete item catalog.
- Complete equipment catalog/tables.
- Complete shop offer catalog and reset rules.
- Complete currency source/sink matrix.
- Summon pools, pity and banners.
- Daily/weekly quests.
- Event objective/reward cycles.
- Mail reward safety.
- Guild contribution/missions/raid/war/store where confirmed.
- Free Gold/Stamina/exchange loops where confirmed.
- Economy property tests and idempotency tests.

**DoD**

- Every item/currency has documented sources and sinks.
- No sample-only catalog is used in release paths.
- Reward replay/double-claim/negative-wallet tests pass.
- M65 merged into `main`.

---

## Phase D — Production mobile UI/UX

### M66 — Mobile design system and navigation hardening

**Objective**

Replace generic shell styling with one production design system before redesigning screens individually.

**Scope**

- Typography scale.
- Color/semantic tokens.
- Buttons/tabs/cards/chips/badges.
- Currency bars.
- Hero rarity/frame components.
- Modal/bottom sheet/toast/tooltip.
- Loading/skeleton/empty/error/offline states.
- Safe area/notch handling.
- Screen transitions.
- Responsive layout rules for common Android aspect ratios.
- Haptic/audio interaction hooks.
- Accessibility/readability minimums.

**DoD**

- Shared components replace duplicated generated shell elements.
- UI can be reskinned/tuned centrally.
- M66 merged into `main`.

---

### M67 — Production screens batch A: core loop

**Screens**

- Home.
- Roster.
- Hero Detail.
- Formation.
- Adventure/Main Quest.
- Battle/result integration.
- Summon.
- Inventory/Equipment.

**DoD**

Each screen has final interaction flow, real data binding, loading/error/empty states, EN/VI support and screenshot regression captures. M67 merged into `main`.

---

### M68 — Production screens batch B: meta/social/live loops

**Screens**

- Arena.
- Shadow Arena.
- Guild.
- Shop.
- Quest.
- Events.
- Mail.
- Settings.
- PvE mode entry screens added in M62.
- Scroll/Tailed Beast/Jinchuriki progression screens added in M64.

**DoD**

The complete release mobile navigation graph has no placeholder screen or dead action. M68 merged into `main`.

---

## Phase E — Production art, animation, VFX and audio

### M69 — Art pipeline freeze and first production package

**Objective**

Prove the full art package standard before scaling to hundreds of variants.

**Package contract**

Every release variant must eventually provide:

- portrait;
- icon;
- chibi battle prefab;
- mandatory animation set;
- VFX set;
- SFX/voice hook set;
- regression capture;
- review evidence;
- `package.json` descriptor with valid repository-relative paths.

**Tasks**

- Freeze naming/import/compression/Addressables standards.
- Freeze sprite/prefab anchor/pivot conventions.
- Freeze Animator state/trigger contract.
- Freeze skill VFX timing contract.
- Freeze mobile texture/audio budgets.
- Finish at least one complete representative package through `READY` and Unity runtime loading.

**DoD**

One package passes every production asset gate and renders correctly in real battle replay. M69 merged into `main`.

---

### M70–M73 — Full 427-package production art rollout

Use the existing manifest/batch structure. Work in small reviewable batches, but each milestone PR must be based on latest `main`.

Suggested milestone split:

- **M70:** batches B01–B11.
- **M71:** batches B12–B22.
- **M72:** batches B23–B33.
- **M73:** batches B34–B43 + global consistency pass.

For every package:

1. create/review portrait and icon;
2. create chibi prefab;
3. import animation set;
4. connect skill/Rage/Awakening VFX;
5. connect SFX/voice hooks;
6. create visual regression capture;
7. review at target mobile scale;
8. mark component `READY` only when real files exist;
9. run art/package/Addressables validators.

**DoD for M73**

- 427/427 release-scope packages tracked.
- 427/427 package components `READY`.
- `validate-production-assets.py` passes.
- `validate-art-packages.py --release` passes.
- No TODO/CONCEPT placeholder is counted as release art.

Note: all artwork must be legally usable/original reconstruction assets; do not commit copied proprietary game assets merely to satisfy a gate.

---

## Phase F — Verification, balance and release quality

### M74 — Reference parity and balance verification

**Objective**

Promote release profiles from experimental assumptions to measured, evidence-backed values.

**Tasks**

- Complete measurement corpora for every required profile.
- Verify damage/stat scaling.
- Verify Rage generation/consumption.
- Verify action timing/cooldowns/casts/recovery.
- Verify status duration/ticks/control.
- Verify summon profile/pity.
- Verify level/progression costs.
- Verify mode limits/rewards/resets.
- Re-run deterministic simulations after tuned values.
- Update feature census states.

**DoD**

- Required release profiles: 100% `VERIFIED`.
- Release-scope features: `PARITY_PASS` or documented intentional divergence.
- No evidence gate is waived.
- M74 merged into `main`.

---

### M75 — Full automated E2E, regression and reliability hardening

**Objective**

Prove the game is safe across complete player journeys, persistence and repeated execution.

**Automated suites**

- fresh account/bootstrap;
- starter formation;
- campaign progression;
- battle/reward application;
- hero level/progression;
- summon/pity/duplicate behavior;
- inventory/equipment;
- progression systems;
- PvE unlock/use/reset;
- Arena/Shadow Arena;
- guild/quests/events/mail;
- Awakening;
- save/restart/reload;
- DB migration fixtures;
- idempotency/concurrency;
- economy invariants;
- deterministic same-seed replay;
- golden battle fixtures;
- Unity EditMode/PlayMode tests;
- screenshot regressions;
- soak/performance tests.

**DoD**

- A complete new-account -> late-game release journey passes.
- No critical/high-severity defect remains open.
- M75 merged into `main`.

---

### M76 — Android performance budgets and real-device certification

**Objective**

Turn the current device evidence format into objective release certification.

**Tasks**

- Define per-device-class FPS/frame-time/memory budgets using real benchmark data.
- Enforce numeric budgets in `validate-mobile-release-evidence.py`; do not rely solely on human `performance_pass=true`.
- Build exact release candidate APK/AAB.
- Test at least two distinct Android device models.
- Cover at least two device classes among LOW/MID/HIGH.
- Record average FPS, p95 frame time, peak memory, smoke result and captures.
- Test install/update, cold start, suspend/resume, network failure/retry and long battle sessions.

**DoD**

- At least two passing real-device evidence rows across >=2 models and >=2 classes.
- Numeric performance budgets pass.
- M76 merged into `main`.

---

### M77 — Production hardening and final mobile release candidate

**Objective**

Create the exact commit that can be tagged as the mobile release candidate.

**Tasks**

- Security/configuration audit.
- Production environment configuration.
- Database migration/rollback rehearsal.
- Backup/restore rehearsal where deployment includes persistent server state.
- Rate limiting/abuse checks for economy/reward endpoints.
- Logging and operational diagnostics appropriate for production.
- Crash/error handling review.
- Final localization sweep.
- Final legal/asset provenance review.
- Final store metadata/icon/splash/versioning package.
- Regenerate signed AAB.
- Run complete release workflow.
- Run `release-audit.py --enforce`.
- Freeze release notes and known issues.

**Final DoD**

All 100% criteria in Section 1 pass on one `main` SHA. Tag that SHA as the release candidate only after all automated and manual evidence is attached.

---

# 5. Cross-cutting engineering requirements

These apply to every milestone.

## Server/API

- Server authoritative for currencies, progression, rewards, combat results and unlocks.
- Validate ownership/cost/attempt/reset rules server side.
- Transactional/idempotent reward application.
- Do not trust Unity-provided reward/result values.
- Version persistence migrations and test upgrade paths.

## Combat

- Keep one deterministic realtime simulation contract.
- No hero-specific branching inside the core battle scheduler.
- Same seed + same content/ruleset must reproduce the same authoritative result.
- Presentation may interpolate/accelerate/pause locally but cannot modify authoritative simulation timestamps/results.

## Content

- Production content is data driven.
- Research uncertainty remains visibly labeled until evidence exists.
- Do not rename inferred values to verified simply to unblock release.

## Unity

- Separate authoritative data state from presentation state.
- Handle loading/error/offline/retry.
- Avoid allocations/per-frame polling where event-driven updates are sufficient.
- Addressables/assets must have stable keys and fallback behavior only in development, not release.

## Localization

- User-facing text must support EN and VI.
- No production UI strings silently hard-coded in one language.

## Assets

- `READY` means concrete files exist and pass import/runtime review.
- Development fallbacks must be impossible to mistake for production completeness.

---

# 6. Quality gates before every merge

A milestone may merge only when all applicable items are true:

- scope matches milestone document;
- branch started from latest `main` or was refreshed before final review;
- no unrelated changes;
- server compile/tests pass where touched;
- relevant static validators pass;
- Unity compile/EditMode/PlayMode tests pass where available and applicable;
- content schemas validate;
- DB migrations have fixture/upgrade coverage;
- no fake release evidence was introduced;
- PR diff reviewed against `main`;
- docs/status updated in the same PR when behavior/status changed;
- known external blockers are stated explicitly;
- after merge, next milestone starts from the new `main`.

For release-sensitive milestones, a CI system outage is not equivalent to a green check. If CI cannot run, code may be prepared but the milestone must not be declared validated/release-complete solely from static inspection.

---

# 7. Completion metrics tracked throughout execution

The release dashboard/status document should track at least:

| Metric | Release target |
|---|---:|
| Release Hero Versions structurally valid | 100% |
| Base skill slots reviewed | 100% |
| Awakening Skills reviewed | 100% |
| Production campaign stages implemented | 100% of frozen census |
| Production PvE modes implemented | 100% of frozen census |
| Production PvP loops implemented | 100% |
| Production mobile screens complete | 100% |
| Art packages tracked | 427 / 427 |
| Art packages fully READY | 427 / 427 |
| Required reference/balance profiles VERIFIED | 100% |
| Release features PARITY_PASS | 100% of parity target |
| Critical/high defects | 0 |
| Full player-journey E2E | PASS |
| Passing Android device evidence | >=2 models, >=2 classes |
| Signed release AAB | PASS |
| `release-audit --enforce` | PASS |

---

# 8. Recommended implementation order

The authoritative order is:

```text
M54 battle presentation
 -> M55 Android build lane
 -> M56 completion baseline
 -> M57 evidence schema/gates
 -> M58-M60 full skill review
 -> M61 campaign
 -> M62 PvE
 -> M63 PvP
 -> M64 progression
 -> M65 economy/live loops
 -> M66 design system
 -> M67-M68 production screens
 -> M69 art package proof
 -> M70-M73 full production art
 -> M74 parity/balance verification
 -> M75 E2E/reliability
 -> M76 real-device certification
 -> M77 final production hardening/release candidate
```

Reason for this ordering:

- lock combat/content semantics before producing hundreds of animations/VFX;
- finish game modes before finalizing every screen;
- freeze UI/art contracts before mass asset production;
- perform final parity/balance after the full gameplay surface exists;
- perform real-device certification on the actual release candidate rather than an early prototype.

---

# 9. Desktop gate

Do not begin the desktop product milestone until M77 mobile release criteria pass. After mobile release, create a separate desktop roadmap that reuses:

- shared Unity content/assets;
- deterministic battle/runtime contracts;
- Java backend;
- localization;
- progression/economy content;
- design tokens where appropriate.

Desktop-specific work must not delay the mobile completion path.
