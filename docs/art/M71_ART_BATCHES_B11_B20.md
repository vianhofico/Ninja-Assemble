# M71 — Art Batches B11–B20

M71 assigns the next 100 deterministic hero-version packages to production. Readiness is derived from repository evidence via `scripts/validate-art-batch-range.py`; there is no manually editable READY count.

Completion requires:

`python scripts/validate-art-batch-range.py --first B11 --last B20 --expected 100 --require-ready`

The range descriptor may merge before the art is complete, but release certification cannot pass until this exact evidence-backed command passes.
