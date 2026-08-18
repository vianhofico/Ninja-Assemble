# M57 Reference Evidence Hardening

M57 modernizes the reference measurement layer around canonical realtime combat and Rage without inventing observations.

## Canonical schemas

The release measurement registry now has one explicit corpus file per release category.

- `ABILITY_CYCLE` uses `rage_before`, `rage_after`, `cooldown_ms`, `cast_time_ms`, and `recovery_ms`.
- `STRUCTURED_EFFECTS` uses `duration_ms` and `tick_interval_ms`; turn-count duration is forbidden.
- `REALTIME_TIMING` has a dedicated `realtime-timing.csv` corpus with attack/cast/recovery/cooldown/battle elapsed timing in milliseconds.
- `RAGE_RULES` has a dedicated `rage-rules.csv` corpus with before/delta/after/cost/ready-state fields.

The two new corpora are intentionally header-only at integration time. Empty corpora are valid only while their profiles remain `EXPERIMENTAL`; no sample or confidence state is fabricated.

## Evidence validator

`scripts/validate-reference-evidence.py` now:

1. defines the schema/file contract for all 10 release categories;
2. requires the profile registry to cover all release categories;
3. rejects missing or unregistered corpus files;
4. rejects legacy energy and `*_turns` columns;
5. requires measurement IDs, context keys, evidence references, and observation timestamps for real rows;
6. ensures every row is stored in the corpus matching its profile category;
7. rejects placeholder/debt tokens such as `UNKNOWN`, `INFERRED`, `RESEARCH_REQUIRED`, `TODO`, and `PLACEHOLDER` from promoted OBSERVED/VERIFIED evidence;
8. preserves threshold enforcement for VERIFIED profiles;
9. never auto-promotes profile confidence.

Current truth remains **0 / 10 VERIFIED**.

## CI and release gate

`reference-evidence-integrity.yml` validates the evidence model on relevant PR changes and renders `release-audit.py --json` without enforcing release completeness during normal development.

`production-release-gate.yml` now explicitly runs:

- `python scripts/validate-reference-evidence.py`
- `python scripts/release-audit.py --enforce`

Therefore the production gate cannot rely only on a manually edited `VERIFIED` status. Schema/evidence thresholds are checked before the aggregate release gate.

## Merge semantics

GitHub Actions may still fail before hosted-runner allocation. M57 is a schema/gate implementation milestone, so it may use the documented non-release CI-outage exception after final diff/source validation. This never changes evidence state: profiles remain EXPERIMENTAL until real measured samples satisfy their thresholds.

## Next

M58 performs the full Hero Version skill identity/canon/editorial pass. It must not promote unresolved source-confidence debt to final review status without evidence.
