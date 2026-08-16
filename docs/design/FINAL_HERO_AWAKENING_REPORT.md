# Final Hero / Awakening Migration Report

## Executive result

The legacy 427-row variant census is no longer treated as a list of independently collectible heroes or as a linear evolution chain.

The production model is now:

```text
Collectible Hero Version
    -> zero or one persistent Awakening
```

Normal Hero Versions expose exactly five base skill identities. Hero Versions that own an Awakening expose one additional sixth Awakening Skill when awakened.

Frame Advance remains a separate rank/progression track. The former `BASE -> variant -> variant -> ...` playable Evolution system has been removed from server APIs, Unity actions and production progression data.

## Source-material classification

M48 CI validates exactly **427 unique legacy source rows**.

Final classification distribution:

| Classification | Source rows |
|---|---:|
| `COLLECTIBLE_HERO_VERSION` | 184 |
| `AWAKENING_FORM` | 58 |
| `SKILL_OR_ULTIMATE` | 49 |
| `TEMPORARY_COMBAT_FORM` | 7 |
| `COSMETIC_SKIN` | 88 |
| `COOPERATION_FORM_OR_TECHNIQUE` | 2 |
| `SPECIAL_INDEPENDENT_CHARACTER` | 9 |
| `MERGED_OR_REMOVED_DUPLICATE` | 30 |
| **Total** | **427** |

The classification is source-material disposition, not a direct row-for-row count of final Hero Versions. M41/M42 also introduced normalized canonical/synthetic base Hero Versions where legacy rows were technique/form labels rather than viable collectible bases. Therefore the final runtime Hero Version count is larger than the 184 source rows classified directly as collectible.

## Final production catalog

- **194 collectible Hero Versions**.
- **60 one-time Awakenings**.
- **134 Hero Versions without a persistent Awakening** at the current research boundary.
- **970 explicit base skill aliases** = 194 × 5.
- **60 explicit Awakening Skills** = exactly one per Awakening.
- **781 legacy compatibility bridge rows** used to preserve old identities/save migration and audited enemy/reference compatibility.
- **0 persistent form collisions** where one form is both a collectible Hero Version and an Awakening.
- **0 multi-stage Awakening chains** in playable runtime.

## Legacy variant dispositions

### Converted to skills / ultimates

49 legacy rows are explicitly classified as `SKILL_OR_ULTIMATE`. These are no longer independently collectible units.

### Converted to temporary combat forms

7 rows are `TEMPORARY_COMBAT_FORM`. They may appear during a skill/passive/Awakening presentation but are not persistent Hero Versions.

### Converted to skins

88 rows are `COSMETIC_SKIN`. They do not create independent ownership/combat identities.

### Cooperation content

2 rows are `COOPERATION_FORM_OR_TECHNIQUE` and remain cooperation content rather than solo canonical forms.

### Special independent characters

9 rows are `SPECIAL_INDEPENDENT_CHARACTER`. They remain explicit special/summon/content identities and are not silently coerced into a normal Hero Version just to satisfy migration coverage.

### Merged/removed duplicates

30 rows are `MERGED_OR_REMOVED_DUPLICATE` after whole-roster review.

## Runtime cutover

M48 removes the obsolete playable linear-evolution implementation:

- `EvolutionApplicationService` removed;
- `EvolutionPathCatalogService` removed;
- `GET /progression/evolution-paths/{characterId}` removed;
- `POST /progression/heroes/{playerHeroId}/evolve` removed;
- legacy `PUT /heroes/{playerHeroId}/variant` gameplay endpoint removed;
- Unity `EVOLVE` action and evolution-path selection removed;
- `game-data/progression/playable-evolution-paths.csv` removed;
- old `game-data/progression/evolution-paths.csv` removed;
- obsolete Evolution path tests removed.

The supported form-progression action is the one-time Awakening flow introduced by M43/M46. Frame Advance remains because it represents rank/frame progression rather than a character-form chain.

## Ownership / acquisition guarantees

- Ownership source of truth: stable `hero_version_id` + `awakened:boolean`.
- A duplicate summon grants the same Hero Version; it does not create an Awakening form as a second unit.
- Awakening forms are not summon-pool entries.
- Legacy variant names may only be translated through the audited compatibility bridge for migration/reference compatibility.
- Production Hero Detail and progression APIs do not expose variant cycling or multi-stage evolution.

## Skill identity status

M47 guarantees structural identity coverage:

- exactly five base slots per Hero Version;
- exactly one sixth skill per Awakening;
- all base aliases resolve to packaged techniques;
- same-character versions cannot silently use an identical complete five-technique effective kit;
- curated M47 version-identity overrides are applied by the runtime catalog.

This does **not** claim final skill-design parity. M47 intentionally preserves explicit research-debt statuses. Mandatory #54/M50 must research and finalize every Hero Version's canon/version-specific five-skill kit, signature Rage Skill, sixth Awakening Skill, real-time timing, balance, counterplay, EN/VI descriptions and cinematic/VFX/SFX contract.

## Visual transformation status

All 60 persistent Awakenings have a visual-spec entry and one Awakening Skill identity. The production art pipeline still controls whether an individual portrait/chibi prefab/animation/VFX/SFX/capture package is actually `READY`; schema coverage must never be confused with finished art assets.

## Validation

M48 final gate verifies:

- 427/427 source rows classified exactly once;
- allowed classification taxonomy only;
- 194 unique Hero Versions;
- 60 unique one-time Awakenings;
- 970 base skill aliases;
- 60 sixth Awakening Skills;
- no collectible/Awakening form collision;
- every source row remains auditable through the compatibility bridge;
- no linear Evolution endpoint/UI/data/service exists in playable runtime;
- Frame Advance still passes its verified early-rank tests;
- Hero runtime catalog tests pass.

The initial M48 CI run reported:

```text
M48_FINAL_CUTOVER_OK variants=427 heroes=194 awakenings=60 base_skills=970 awakening_skills=60 bridge_rows=781
classifications=AWAKENING_FORM=58,COLLECTIBLE_HERO_VERSION=184,COOPERATION_FORM_OR_TECHNIQUE=2,COSMETIC_SKIN=88,MERGED_OR_REMOVED_DUPLICATE=30,SKILL_OR_ULTIMATE=49,SPECIAL_INDEPENDENT_CHARACTER=9,TEMPORARY_COMBAT_FORM=7

HERO_PROGRESSION_OK frame=retained form_upgrade=one-time-awakening linear_evolution=removed
```

Focused Java tests: 6 tests, 0 failures, 0 errors.

## Unresolved research / design work

These are deliberately not fabricated in M48:

1. Final canon and balance review for all M47 baseline skill aliases.
2. Final continuous-time combat timing, Rage gain, SPD/action-frequency and status-duration values.
3. Final signature Rage Skill selection/cinematic design for every Hero Version.
4. Final mechanics for all sixth Awakening Skills.
5. Remaining lore questions where M41 confidence is `DESIGN_INTERPRETATION` rather than direct high-confidence canon evidence.
6. Production-ready chibi/art/VFX/audio packages and device evidence.

Those are mandatory follow-up work, not reasons to restore legacy variant chains.

## Mandatory next milestones

1. **#51 / M49** — migrate the whole combat engine from turn/round semantics to deterministic continuous-time auto combat and implement the Rage 0–100 runtime foundation.
2. **#54 / M50** — complete full Hero Version skill research/design/balance/cinematics on top of the M49 runtime.

The overall project is not considered complete until both milestones are implemented, tested, merged and green.
