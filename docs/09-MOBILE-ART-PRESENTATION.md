# Mobile Art & Presentation — Chibi Ninja Assemble Target

## Release quality bar

The mobile release must read visually like a polished 2D chibi Naruto collection RPG, with the same visual language as the selected Ninja Assemble reference build while using a clean-room asset pipeline.

No placeholder portrait, battle body, animation set or VFX set may remain in a release candidate.

## Character proportions

- battle character height: roughly **2–2.5 heads**;
- oversized head and eyes, compact torso, short limbs;
- recognizable hairstyle, headband, cloak, weapon and eye-technique silhouette are prioritized over anatomical detail;
- strong dark readable outline at mobile scale;
- mostly flat/cel-shaded color blocks with one primary shadow group and small highlight accents;
- face remains readable when the character occupies ~12–18% of screen height.

## Working resolutions

- portrait master: 2048×2048; exported addressable portrait: 1024×1024 minimum;
- roster icon: 512×512 source, 256×256 runtime target where appropriate;
- chibi body parts/sprite sheets authored at 2× expected runtime resolution;
- ultimate cut-in: 1920×1080-safe composition or larger master;
- VFX textures: power-of-two atlases; use additive/alpha blend deliberately.

## Mandatory animation states per release variant

1. Idle
2. Entrance
3. Move
4. BasicAttack
5. Skill01
6. Skill02 / secondary active where applicable
7. Ultimate
8. Hit
9. CriticalHit or heavy-hit reaction
10. Buff
11. Debuff/control
12. Death
13. Victory
14. Revive when supported
15. Transform when the variant can transform in battle

A variant may alias states only if the reference presentation genuinely does so; aliases must be declared in its manifest.

## Skill presentation language

- ability silhouettes are intentionally oversized relative to the chibi body;
- impact frame uses hit-stop, shake and a high-contrast flash where appropriate;
- screen-space damage numbers are separated from character sprites;
- ultimates may use cut-ins, camera push/zoom, background dimming and full-screen VFX;
- animation timing is driven from the server battle timeline but presentation can interpolate between deterministic events.

## Asset addressing

Every variant gets stable addresses:

```text
heroes/{character}/{variant}/portrait
heroes/{character}/{variant}/icon
heroes/{character}/{variant}/prefab
animations/{character}/{variant}
vfx/{character}/{variant}
sfx/{character}/{variant}
```

Addressables are mandatory for character presentation content so hundreds of variants can be split into local content groups without hard references.

## Visual regression

For every `READY` variant capture:

- roster card;
- hero detail idle pose;
- battle idle;
- basic attack impact;
- each active/ultimate climax frame;
- evolution/transform view when applicable.

Reference and actual captures are indexed by variant ID. A release candidate cannot mark a variant `READY` without those captures.
