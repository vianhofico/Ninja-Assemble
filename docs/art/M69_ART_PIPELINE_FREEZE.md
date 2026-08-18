# M69 — Production Art Pipeline Freeze

M69 freezes the repository contract used by the 427-package rollout. It does **not** promote concept art, placeholders or missing assets to production readiness.

## Frozen package contract

A production package consists of portrait, icon, chibi prefab, animation set, VFX set, SFX set, regression capture and human review evidence. The canonical contract is `art/pipeline/art-package-contract-v2.json`, Addressables naming is frozen in `art/pipeline/addressables-layout.csv`, prefab hierarchy in `art/pipeline/PREFAB-HIERARCHY.md`, performance budgets in `art/pipeline/performance-budgets.csv`, and review evidence in `art/pipeline/review-evidence.schema.json`.

A package may be promoted to `READY` only when every descriptor path resolves to a real repository file, Addressables/runtime loading succeeds without fallback assets, the chibi/presentation sets render in a real battle replay, regression captures and human review evidence exist, and all eight component fields are `READY`. Only then may the package manifest be promoted to `READY`.

## Frozen runtime contract

Required Animator states are `Idle`, `Move`, `Attack`, `Hit`, `KO`, `Skill`, `RageSkill`. The prefab hierarchy provides stable `VisualRoot`, `VfxSockets`, `UiAnchor`, `AudioRoot` and runtime binding points. Prefabs remain presentation-only and cannot own authoritative combat logic.

## Frozen mobile performance budgets

`art/mobile-asset-budgets.json` and `art/pipeline/performance-budgets.csv` define Android texture, animation, VFX, audio and installed-size guardrails. These are engineering constraints, **not** evidence that existing assets already satisfy them.

## Batch rollout contract

The release census remains 427 hero-version packages, split deterministically into 43 batches: B01-B42 contain 10 packages and B43 contains 7. `scripts/generate-art-batch-plan.py` validates this census without inventing package readiness.

## Validation

`python scripts/validate-m69-art-pipeline.py` validates the single M69 contract, Addressables layout, Rage animator contract, prefab hierarchy, evidence schema, mobile budgets and deterministic 43-batch census. `python scripts/validate-art-packages.py` remains the package-state authority.

## Milestone boundary

M69 is complete when this pipeline contract is frozen and validated. **Real package production is M70–M73.** Therefore M69 may merge while the art census remains 0/427 READY; no downstream milestone or release gate may reinterpret M69 completion as art completion.
