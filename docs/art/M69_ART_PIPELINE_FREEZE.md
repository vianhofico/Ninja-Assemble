# M69 — Production Art Pipeline Freeze

M69 freezes the repository contract used by the 427-package rollout. It does **not** promote concept art or placeholder assets to production readiness.

## Canonical representative package

The proof package is `naruto-uzumaki / Sage Mode`. It remains non-READY until every repository-backed component and review artifact exists and the package has been exercised in a real Unity battle replay.

## Frozen package contract

A production package consists of portrait, icon, chibi prefab, animation set, VFX set, SFX set, regression capture and human review evidence. The canonical address/repository layout is defined by `art/art-production-contract.json`; mobile asset constraints are defined by `art/mobile-asset-budgets.json`.

A package may be promoted to `READY` only when:

1. every descriptor path resolves to an actual repository file;
2. Addressables/runtime loading succeeds without fallback assets;
3. the chibi prefab and presentation sets render in a real battle replay;
4. a repository-backed regression capture exists;
5. human review evidence exists;
6. all eight component fields in `hero-art-component-status.csv` are `READY`;
7. only then may `hero-art-manifest.csv` be promoted to `READY`.

## Frozen Android budgets

- ASTC textures; portrait max 1024, icon max 512, chibi/UI textures max 2048.
- Hero animation controllers: maximum four layers; battle clips maximum 12 seconds.
- Hero VFX: maximum 300 particles per effect, 12 concurrently active hero systems, eight-second effect ceiling, overdraw review required.
- SFX: Vorbis, 44.1 kHz, eight-second ceiling; music is the only streaming class.
- Installed hero-variant package budget: 12 MiB.

These are engineering guardrails, not evidence that current assets satisfy them.

## Validation

`python scripts/validate-m69-art-pipeline.py` validates the frozen contract and prevents metadata from declaring the representative package READY without component evidence. `python scripts/validate-art-packages.py` remains the package-state authority.

## Remaining Definition-of-Done blocker

The repository currently contains no completed descriptor/assets for the representative package under `art/packages/naruto-uzumaki/sage-mode/` and no real Unity battle replay capture/review evidence. M69 therefore cannot be merged as complete until those real assets and evidence are supplied and verified. This document intentionally does not fabricate them.
