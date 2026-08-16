# M22 — Runtime 5v5 Chibi Battle Stage

## Goal

Turn the deterministic battle result into a real mobile 5v5 presentation layer that can consume production chibi Addressables as art packages become READY, while keeping a clear non-production fallback for unfinished variants.

## Protocol extension

`POST /api/v1/play/{playerId}/battle` now returns a `participants` array next to the deterministic `battle` result.

Each participant contains:

- `battleUnitId` — exact identifier referenced by timeline events;
- `characterId`;
- display name;
- variant/form;
- level;
- side A/B;
- slot 0–4;
- max HP from the exact `BattleUnitSeed` used by simulation.

The simulation result itself remains unchanged. Participant metadata exists only so clients do not have to infer a hero identity from UUID/enemy IDs or duplicate combat-stat calculations.

## Unity runtime stage

`BattleVisualStage` creates mirrored five-slot formations and registers a `BattleActorView` for all ten participants.

Current visual event support follows the existing deterministic protocol:

- `ATTACK` → Attack animator/audio hook
- `DAMAGE` → current HP mutation + hit/critical reaction + floating damage number
- `KO` → death state
- playback completion → victory trigger for living actors on the winning team
- critical damage → short presentation-only screen shake

`BattleActorView` now owns runtime HP state and optional name/level/slider/audio references. Production prefabs can provide their own Animator, visual hierarchy and audio clips while keeping the same generic battle contract.

## Production art vs fallback

The Unity runtime art catalog is generated from `art/manifests/hero-art-manifest.csv`.

A production prefab is loaded only when its manifest entry is `READY`. The prefab must contain:

- `RectTransform`
- `BattleActorView`
- animator/audio/UI references as needed by that package

If a variant is `CONCEPT`, `TODO`, untracked, missing in Addressables or structurally invalid, the stage deliberately falls back to a simple team-colored actor card with initials, name, level and HP bar. This lets the game flow remain testable without falsely representing placeholder art as finished chibi production.

## Runtime catalog

- source: `art/manifests/hero-art-manifest.csv`
- generator: `scripts/generate-unity-art-runtime-catalog.py`
- Unity resource: `client-unity/Assets/Resources/Generated/hero-art-runtime-catalog.json`

CI compares generated JSON semantically against the committed runtime resource so custom addresses such as Sasuke EMS cannot silently drift.

## Validation

`validate-battle-visual-stage.py` verifies:

- participant fields exist on server and Unity DTOs;
- timeline still handles ATTACK/DAMAGE/KO and applies damage;
- the runtime stage contains two five-slot formations, READY-only Addressables loading and fallback presentation;
- the committed runtime catalog identities/addresses/statuses match the human-managed art manifest.

Java tests validate participant identity, side/slot and HP constraints.

## Important remaining limitation

The current deterministic `BattleEvent` does **not** carry a `skillId`, `abilityId` or effect identifier. Therefore M22 intentionally presents generic attack/hit events and does not pretend to know which Rasengan/Chidori/Susanoo effect to play.

M23 should extend the battle event contract with stable ability/effect identifiers and upgrade the deterministic engine to emit skill/ultimate actions. That is the bridge from a functional 5v5 stage to the diverse character-specific skill presentation required by the final game.
