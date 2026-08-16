# Master Plan — Observable-Parity Replica

## Target

Build a private clean-room replica of the observable Ninja Assemble / Ninja Rebirth experience, then expand it beyond the original roster.

### Mobile release requirements

1. Preserve all observable reference-build gameplay/content/UI behavior.
2. Include all required named, combat-relevant characters from **Naruto** and **Naruto Shippuden**, not only the original game's roster.
3. Model materially distinct forms as separate playable variants when their skill kit, role, transformation, animation/VFX identity or progression differs.
4. Ship all player-facing content in **English (`en`) and Vietnamese (`vi`)**.
5. Do not mark a hero complete until gameplay plus portrait/icon/chibi prefab/animation/VFX/audio/localization gates pass.
6. Finish mobile first; then execute the desktop roadmap using the same Unity project, backend, data and assets.

## Architecture decisions

- Client: Unity, 2D chibi presentation, data-driven prefabs and timelines.
- Server: Java 21 + Spring Boot 4.1 modular monolith.
- Persistence: PostgreSQL; Redis for fast state/cache.
- Battle: deterministic simulation with seed and replayable timeline.
- Content: JSON/CSV-driven; no hero-specific battle-engine branching.
- Hero identity: `Character -> Variant/Form -> HeroDefinition -> SkillKit -> PresentationSet`.
- Localization: `en` + `vi` required for every release row.
- Art gate: placeholders are allowed during development but never count as release-complete.

## Research confidence

- `OFFICIAL_CONFIRMED`
- `COMMUNITY_CONFIRMED`
- `OBSERVED`
- `INFERRED`
- `UNKNOWN`

Unknown reference-specific formulas stay configurable until verified.

## Delivery

### R0 — Reference + expanded roster census

- screen/feature census;
- original hero/variant census;
- expanded Naruto/Shippuden character master census;
- concrete form/variant census;
- skill/progression/item/shop/stage census;
- visual/audio evidence index.

Exit: every original visible item is represented, and every required Naruto/Shippuden character has an explicit completion row.

### R1 — Exact rules specification

Lock verified rules for battle scheduling, energy, damage, status, formation/targeting, Frame Advance, Six Path/Awakening, Tailed Beast, Scroll/Ninja College, modes, shops and resets.

### M0 — Bootstrap ✅

Server, DB/Redis, Unity shell, CI, initial parity structures.

### M1 — Player/economy primitives ✅

Guest/local account, profile/EXP, energy, wallet ledger, game clock/reset.

### M2 — Expanded hero catalog + ownership

- character master catalog;
- unlimited variants/forms;
- VI/EN name/title/skill-description keys;
- soul/shard ownership;
- hero level/XP;
- skill definitions;
- role/archetype/position;
- art/content completion states.

### M3 — Deterministic battle engine

Seed/ruleset, teams of five, action scheduling after verification, physical/chakra channels, energy, targeting, effects, passive triggers, timeline/replay and golden tests.

### M4 — Hero progression/evolution

Level/EXP, Frame Advance, skill levels, Scroll/Ninja College, Tailed Beast/Jinchuriki, Six Path+, transformations and Awakening.

### M5–M13 — Game modes/economy

Main Quest; Ninja Trial/Forest Hunt/Tailed Beast/Ninja Quest/Crusade; Arena/Shadow Arena; team synergy; shops; Guild; dailies/events.

### M14 — Chibi presentation

- 2–2.5 heads tall;
- large expressive head/eyes, short limbs;
- clean dark anime outline and cel shading;
- high-resolution reconstruction;
- oversized skill VFX and strong hit feedback;
- final portrait/icon/prefab/animation/VFX/audio set per playable variant.

### M15 — Full expanded content import

- every original observed hero/variant;
- expanded Naruto/Shippuden master roster and approved variants;
- all required skills/passives;
- English + Vietnamese localization;
- bosses, Bijuu, stages, items, shops and events.

### M16 — Mobile completion audit

Golden battle/economy/progression tests, localization completeness, screenshot/animation regression, full E2E, and per-hero art gate.

### D0–D6 — Desktop after mobile

Execute `docs/07-DESKTOP-AFTER-MOBILE.md`; reuse the same core project/data/assets.

## Hero definition of done

A required playable variant is complete only when:

1. census/source evidence exists;
2. EN + VI strings resolve;
3. skills/progression are documented and implemented;
4. deterministic tests pass where applicable;
5. portrait + icon + chibi prefab + animations + VFX + audio are final;
6. visual QA/regression passes;
7. state is `CONTENT_COMPLETE`.
