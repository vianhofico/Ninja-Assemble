# M70 — Art Batches B01–B10

M70 owns the first 100 hero-version packages from the deterministic M69 census. Batch membership is derived from sorted `(character_id, variant)` census keys; B01–B10 contain exactly 100 packages.

## Production rule

M70 does not store a manually editable `ready_count`. `scripts/validate-art-batch-range.py` derives readiness from `hero-art-component-status.csv` and re-runs `validate-art-packages.py`, so a package counts only when all eight M69 component gates are READY and the repository-backed package rules remain valid.

## Batch state

`art/batches/M70_B01_B10.json` assigns the range and expected package count. `productionState=ACTIVE` means the batch is assigned to production; it does **not** mean the art is complete.

## Completion

M70 art completion requires:

`python scripts/validate-art-batch-range.py --first B01 --last B10 --expected 100 --require-ready`

This command is intentionally not weakened for CI outages or missing art. Until it passes with real files/evidence, the release dashboard must report M70 art debt truthfully.
