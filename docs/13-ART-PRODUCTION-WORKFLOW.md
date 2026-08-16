# Art Production Workflow — 427 Variant Queue

## Objective

Convert every playable variant from census/gameplay-ready into presentation-ready without lowering the approved Ninja Assemble-like chibi quality bar.

## Batch order

1. **Flagship vertical slice:** Naruto, Sasuke, Sakura, Kakashi, Itachi, Madara, Obito, Gaara, Minato, Hashirama, Nagato/Pain, Guy.
2. **Original/reference-priority variants:** rows from `variant-census.csv`.
3. **Complete Roster+ expansion variants:** rows from `variant-census-expanded.csv`.
4. Polish/regression pass across the complete collection.

Run `python scripts/art-production-queue.py` to generate the current ordered queue.

## Per-variant production stages

`TODO → CONCEPT → IN_PROGRESS → REVIEW → READY`

A variant reaches `READY` only after all of the following exist and have been reviewed:

- 2048 master / >=1024 runtime portrait;
- 512 master roster icon;
- 2–2.5-head 2D chibi battle body/prefab;
- Idle, Entrance, Move, BasicAttack, Skill01, Skill02 where applicable, Ultimate, Hit, heavy/critical hit, Buff, Debuff/control, Death, Victory and form-specific Revive/Transform where needed;
- VFX timeline for every active/ultimate;
- SFX/voice hook set;
- hero detail and battle visual-regression captures;
- stable Addressables addresses already defined by the manifest convention.

## Quality criteria

- readable silhouette at mobile battle scale;
- strong controlled outline and mostly flat/cel-shaded palette;
- facial identity readable despite super-deformed proportions;
- no inconsistent head/body scale between characters;
- signature technique VFX may exceed the character silhouette substantially;
- skill impact timing includes hit-stop/camera response where appropriate;
- variants must differ visually and in kit when the form represents a meaningful power-state change.

## Release rule

Do not mass-mark rows `READY`. The strict release validator requires real presentation coverage for every census variant, and the art audit is designed to make missing packages visible.
