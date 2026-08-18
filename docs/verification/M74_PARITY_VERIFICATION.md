# M74 — Balance and Parity Verification

M74 separates **implementation completeness** from **evidence-backed parity**. A feature can be implemented and playable while still remaining `PENDING_EVIDENCE`.

## Reference profiles

The release requires exactly ten evidence profiles: combat stats, damage formula, summon profile, level cost, ability cycle, structured effects, technique mapping, passive lifecycle, realtime timing and Rage rules. Production parity requires every row in `game-data/reference/balance-profiles.csv` to be `VERIFIED`.

## Feature census

`game-data/release/m74-parity-census.csv` tracks nine implemented release families. `PARITY_PASS` must never be inferred from an implementation milestone or a green structural validator; it requires the corresponding measured/reference review to be approved.

## Skill gates

M74 composes M58 identity, M59 realtime mechanics and M60 balance/presentation review. Normal mode reports truthful debt. `--enforce` executes all three strict gates and fails if any full-roster review remains incomplete.

## Commands

Development/dashboard:

`python scripts/validate-m74-parity.py`

Production certification:

`python scripts/validate-m74-parity.py --enforce`

The second command is expected to remain red while profiles are EXPERIMENTAL or feature rows are `PENDING_EVIDENCE`. That red state is a correct release signal, not a reason to fabricate evidence.
