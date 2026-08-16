# Master Plan — Observable-Parity Replica

## 1. Target

Build a private clean-room replica of the **observable** Ninja Assemble / Ninja Rebirth experience, including battle rules, progression, modes, economy, UI flow, chibi presentation, animation/VFX hooks and content catalog.

“100%” means **all behavior that can be observed and measured in the chosen reference build is present and passes the parity matrix**. It does not mean copying hidden source code or guessing server formulas that cannot be measured.

## 2. Non-negotiable architecture decisions

- **Client:** Unity, 2D chibi presentation, data-driven prefabs and animation/VFX timelines.
- **Server:** Java 21 + Spring Boot 4.1, modular monolith first.
- **Persistence:** PostgreSQL.
- **Fast state/cache:** Redis.
- **Battle:** deterministic simulation with a seed and replayable event timeline.
- **Content:** JSON/CSV-driven; no hero-specific `if/else` in the battle engine.
- **Parity:** every screen/hero/skill/progression rule has a census row and a verification state.

## 3. Research confidence model

Every rule is labeled:

- `OFFICIAL_CONFIRMED` — official store/developer description.
- `COMMUNITY_CONFIRMED` — repeatedly documented by player guides/screenshots/gameplay.
- `OBSERVED` — captured directly from reference footage/screenshots.
- `INFERRED` — plausible but not yet measured; **must not be hard-coded as final parity behavior**.
- `UNKNOWN` — requires census work.

## 4. Delivery phases

### R0 — Reference census

Deliverables:

- screen map;
- feature census;
- hero/variant census;
- skill census;
- progression/evolution census;
- item/scroll/equipment census;
- shop/currency census;
- stage/mode census;
- screenshot/video reference index.

Exit condition: every visible menu in the reference build is represented by a census item.

### R1 — Rules specification

Document exact rules for:

- 5-unit Arena formation;
- 15-unit / 3-team Shadow Arena and best-of-three resolution;
- Chakra, Physical and Hybrid damage archetypes;
- energy/chakra, speed and control effects;
- Main Quest Normal → Elite → Heroic unlock chain;
- Ninja Trial restrictions;
- Forest Hunt rewards;
- Crusade progression;
- Ninja Quest ranks/rewards;
- Frame Advance;
- Tailed Beast/Jinchuriki progression;
- Six Path / Awakening / character transformation requirements;
- Scroll slots, element pairing and upgrades;
- Team bonds/synergy;
- shops and currencies;
- daily/event loops.

Exit condition: no implementation-critical rule remains `UNKNOWN` for MVP systems.

### M0 — Repository/bootstrap (this PR)

- server skeleton;
- DB/Redis;
- first verified battle/progression domain rules;
- Unity shell;
- CI;
- parity data structures.

### M1 — Player and account state

- guest/local account;
- player profile/level/EXP;
- currencies;
- stamina/energy;
- server clock and daily reset boundaries.

### M2 — Hero catalog + ownership

- hero definition schema;
- soul/shard ownership;
- hero level and XP book consumption;
- active/passive skill definitions;
- role/archetype/position metadata;
- hero roster filter/sort.

### M3 — Battle engine v1

- deterministic seed;
- teams of five;
- speed-based action queue only after reference validation;
- physical/chakra damage channels;
- energy resource;
- target selectors;
- buffs/debuffs/control;
- active/passive triggers;
- event timeline for Unity playback.

### M4 — Hero progression

Separate subsystems:

1. level/EXP;
2. Frame Advance: Genin → Chunin → Jonin → Kage → Six Path → Awakening;
3. skill level;
4. Scroll/Ninja College progression;
5. Tailed Beast/Jinchuriki progression;
6. Six Path+ and transformation/evolution requirements.

### M5 — Main Quest

- chapter/stage model;
- Normal/Elite/Heroic gates;
- stamina cost;
- first-clear and repeat rewards;
- sweep behavior after it is verified;
- enemy wave definitions.

### M6 — Resource PvE

- Ninja Trial: Authentic Waterfall, Gama Temple, Path of Kunoichi;
- Forest Hunt: Hunt Forest, Land of Pain;
- Tailed Beast Conquer;
- Ninja Quest;
- Challenge;
- Crusade.

### M7 — PvP

- Arena: one 5-ninja defense formation, asynchronous opponent snapshot, auto ultimate behavior after validation, ranking/reward cycle;
- Shadow Arena: 15 ninjas → three 5-unit squads → best-of-three;
- separate Arena and Shadow currencies/stores.

### M8 — Team synergy

- relationship/team groups (for example, canon team groupings as represented by the reference game);
- frame-level-driven team attributes where verified;
- UI for team activation and bonus preview.

### M9 — Tailed Beast

- beast progression to Kyubi tier as represented by reference;
- soul and beast-bone requirements;
- per-hero Jinchuriki state;
- Six Path prerequisites;
- Tailed Beast battle content.

### M10 — Scroll/Ninja College

- Training;
- Inlay Scroll;
- Combine/upgrade;
- Yin-Yang slot;
- elemental slots;
- max scroll level and stat mapping;
- scroll acquisition tables.

### M11 — Shops/economy

- Diamond, Gold, Arena Coin, Hero/Ninja Coin, Guild Coin, Shadow Coin;
- Ninja Store;
- Mystic Vendor;
- 6-Path Shop;
- Arena Store;
- Crusade/Ninja Store naming normalized after screenshot census;
- Guild Store;
- Shadow Store;
- Boruto Store / redundant soul exchange if present in target build.

### M12 — Guild

- guild membership;
- contribution;
- guild missions;
- guild war;
- guild rewards/store.

### M13 — Daily/events

- Daily Quest;
- Gold Spent;
- Diamond Spent;
- Arena/Shadow battle events;
- Ninjutsu Training;
- Free Gold/Stamina;
- exchanges/roulette where present in target build.

### M14 — Chibi UI/presentation

Style target:

- 2D super-deformed characters, roughly 2–2.5 heads tall;
- large head/eyes, short limbs, strong silhouette;
- flat/cel shading and dark readable outlines;
- exaggerated combat poses;
- skill VFX deliberately larger than body silhouette;
- original high-resolution reconstruction assets, plugged in externally.

### M15 — Complete content census import

- all heroes and variants observed in target build;
- all skills/passives;
- all enemies/bosses;
- all scrolls/items;
- all stages/rewards;
- all shops/events.

No row may remain `DISCOVERED` at release.

### M16 — Parity validation

- golden battle tests;
- economy tests;
- screenshot regression tests;
- animation timing comparisons;
- progression-path tests;
- full new-account → late-game E2E path.

## 5. Definition of parity complete

A feature passes only when:

1. reference evidence is attached;
2. rules are documented;
3. server/client behavior is implemented;
4. deterministic tests pass where applicable;
5. visual/flow comparison passes;
6. parity row is `PARITY_PASS`.

## 6. First implementation sequence after bootstrap

1. Finish the reference census.
2. Implement player profile + wallet + stamina.
3. Implement hero ownership and level-up.
4. Implement battle timeline with five-unit formation.
5. Implement Frame Advance and scroll systems.
6. Implement Main Quest.
7. Implement Arena and Shadow Arena.
8. Expand into the remaining modes.

This order establishes the reusable combat/progression loop before content multiplication.
