# M69 — Production Art Pipeline Freeze

M69 freezes the repository contract used by the 427-package rollout. It does **not** promote concept art or placeholder assets to production readiness.

## Canonical representative package

The proof package is `naruto-uzumaki / Sage Mode`. It remains non-READY until every repository-backed component and review artifact exists and the package has been exercised in a real Unity battle replay.

## Frozen package contract

A production package consists of portrait, icon, chibi prefab, animation set, VFX set, SFX set, regression capture and human review evidence. The canonical contract is `art/pipeline/art-package-contract-v2.json`, Addressables naming is frozen in `art/pipeline/addressables-layout.csv`, prefab hierarchy in `art/pipeline/PREFAB-HIERARCHY.md`, performance budgets in `art/pipeline/performance-budgets.csv`, and review evidence in `art/pipeline/review-evidence.schema.json`.

A package may be promoted to `READY` only when every descriptor path resolves to a real repository file, Addressables/runtime loading succeeds without fallback assets, the chibi/presentation sets render in a real battle replay, regression captures and human review evidence exist, and all eight component fields are `READY`. Only then may the package manifest be promoted to `READY`.

## Frozen mobile performance budgets

The canonical numeric values are intentionally stored in `performance-budgets.csv` so validators and future batch tooling share one source of truth. Current caps include portrait/chibi longest edge 2048 px, icons 512 px, four runtime materials, 128 bones, 12 concurrent particle systems and 500 peak particles per Rage Skill, 12-second skill audio, and 160 active prefab GameObjects excluding pooled global battle FX.

These are engineering guardrails, not evidence that current assets satisfy them.

## Batch rollout contract

The release census remains 427 hero-version packages, split deterministically into 43 batches: B01-B42 contain 10 packages and B43 contains 7. `scripts/generate-art-batch-plan.py` validates this census and can materialize the batch plan without inventing package readiness.

## Validation

`python scripts/validate-m69-art-pipeline.py` validates the frozen contract and prevents metadata from declaring the representative package READY without component evidence. `python scripts/validate-art-packages.py` remains the package-state authority.

## Remaining Definition-of-Done blocker

The repository currently contains no completed descriptor/assets for the representative package under `art/packages/naruto-uzumaki/sage-mode/` and no real Unity battle replay capture/review evidence. M69 therefore cannot be merged as complete until those real assets and evidence are supplied and verified. This document intentionally does not fabricate them.
