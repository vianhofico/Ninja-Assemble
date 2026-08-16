# M20 — Reference Evidence & Balance Verification Harness

## Purpose

M17 deliberately shipped a playable vertical slice with four development profiles whose values are useful for implementation/testing but are **not claimed to match the selected Ninja Assemble reference build**:

- `experimental-combat-stats-v1`
- `experimental-v1-unverified-formula`
- `complete-roster-experimental-v1`
- `experimental-level-cost-v1`

M20 turns that warning into an enforceable engineering gate.

## Confidence states

- `EXPERIMENTAL` — runnable development values; zero measurements are allowed.
- `OBSERVED` — at least one concrete reference measurement exists, but evidence is not yet sufficient for parity claims.
- `VERIFIED` — the declared minimum sample count, distinct contexts and independent evidence references are all satisfied.

Confidence is never promoted automatically. A human/research workflow measures the reference build, records rows and explicitly updates the profile registry only after the corpus is sufficient.

## Registry

`game-data/reference/balance-profiles.csv` is the source of truth for release-critical profile IDs and evidence thresholds.

Runtime code uses `ReferenceProfiles` constants for the same IDs so battle/summon/upgrade version strings cannot silently drift from the registry.

## Measurement datasets

`game-data/reference/measurements/` contains structured, initially header-only datasets:

- `combat-stats.csv`
- `battle-damage.csv`
- `summon-samples.csv`
- `level-cost.csv`

Do **not** add inferred or generated samples. Every row requires:

- globally unique `measurement_id`;
- registered `profile_id`;
- a `context_key` that identifies the tested setup;
- an `evidence_ref` pointing to the screenshot/video/capture/log used to read the value;
- the relevant observed value(s).

## Verification rules

`python scripts/validate-reference-evidence.py` checks:

1. unique profile IDs and valid confidence states;
2. non-negative thresholds;
3. globally unique measurement IDs;
4. every measurement references a known profile;
5. every measurement has context + evidence reference;
6. `OBSERVED` cannot have zero samples;
7. `VERIFIED` cannot pass until all declared thresholds are satisfied.

The same semantics are modeled and unit-tested in Java through:

- `ReferenceConfidence`
- `BalanceProfileDescriptor`
- `ReferenceEvidenceSample`
- `ReferenceEvidenceGate`

## Release gate

`scripts/release-audit.py` now requires **all registered release-critical reference profiles to be `VERIFIED`** in addition to roster/content/art gates.

This means finishing 427 art packages cannot accidentally produce a false “release ready” result while combat math or summon rates are still experimental.

## Recommended measurement workflow

### Combat stats

Capture multiple characters/forms at multiple levels. Vary archetype, rarity/form tier and progression state so contexts are genuinely distinct.

### Damage formula

Use fixed attacker/defender states and record normal + critical hits across Physical and Chakra channels. Keep each reference capture linked to the exact setup.

### Summon

Prefer displayed official/reference rates when visible. When estimating through pulls, keep raw pull rows and pity position; never infer rates from a tiny sample.

### Level cost

Record consecutive level transitions across low/mid/high levels and multiple characters/forms to determine whether costs depend only on level or additional state.

## Completion condition

M20 code is complete when CI enforces the harness. **Reference tuning itself remains incomplete** until real measurements populate the datasets and every profile is explicitly promoted to `VERIFIED`.
