# M18 — Unity Mobile Scene Shell

## Goal

Turn the M17 code-only playable vertical slice into a reproducible Unity mobile scene flow without hand-maintaining opaque `.unity` YAML.

## Generated scene set

Running **Ninja Assemble → Mobile → Generate Complete Scene Shell** inside Unity creates:

- Bootstrap
- Home / Hidden Village
- Ninja Roster
- Hero Detail
- Formation
- Adventure
- Battle
- Summon
- Arena
- Shadow Arena
- Guild
- Shop
- Inventory
- Quest
- Events
- Mail
- Settings

The same command updates Unity Build Settings in deterministic order and creates a local `GameApiConfig` asset if missing.

## Runtime wiring

The generated screens contain:

- 1920×1080 landscape CanvasScaler reference;
- runtime safe-area fitting;
- shared resource HUD for Gold, Diamond and Energy;
- reusable dark/cel-shaded mobile shell palette ready for chibi art overlays;
- bottom Village / Ninja / Team / Adventure / Summon navigation;
- live M17 player state;
- live battle execution on Battle/Adventure;
- live Complete Roster+ summon on Summon;
- live level-up exercise on Hero Detail.

No production character art is faked by this shell. Art is loaded later through the existing Addressables presentation layer.

## Art-manifest scaling

`scripts/generate-full-art-manifest.py` merges the reference and expanded variant census, preserves any existing artist-authored manifest rows, and generates deterministic TODO Addressables contracts for every missing variant. CI generates a full candidate manifest and verifies that it contains no duplicate keys and covers the expanded roster scale.

This does **not** mark assets READY. Release readiness still depends on reviewed portrait/icon/chibi/animation/VFX/SFX/capture packages.

## Validation

- `scripts/validate-unity-shell.py` statically verifies required scene-source components and the complete mobile screen recipe.
- `content-integrity` runs the shell validator and full art-manifest candidate generation on every PR/push.
- Unity menu command **Validate Scene Shell** checks that generated scene assets exist after running the builder locally.

## Next

M19 should focus on flagship visual production + production prefabs for the first 12 priority variants, then roll the same package contract across the full 427-variant queue. Reference-value tuning remains a separate evidence-driven track.
