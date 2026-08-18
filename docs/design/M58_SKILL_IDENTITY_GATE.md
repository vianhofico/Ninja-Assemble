# M58 Full-Roster Skill Identity / Canon / Editorial Gate

M58 turns the skill-design candidate into an auditable full-roster identity review instead of treating generated aliases as final canon.

## Why this gate is necessary

`hero-version-skills.csv` contains 194×5 explicit aliases, but most rows are `PLAYABLE_DESIGN_BASELINE` seeds. The M50 generator correctly labels unreviewed output `RESEARCH_REQUIRED`. Existing deep overrides (for example reviewed Naruto/Sasuke versions) are valuable, but they do not justify mass-promoting the rest of the roster.

The Awakening Skill registry similarly has 60 structural sixth-skill rows, many still carrying `UNRESOLVED_EXPLICIT_DESIGN` / `M47_EXPLICIT_DESIGN_REQUIRED` mechanics or generic signature identity.

M58 therefore does **not** rewrite unresolved rows to READY.

## `validate-m58-skill-identity.py`

Normal audit mode:

- generates the canonical M50 five-slot candidate;
- runs the existing M50 structural validator;
- requires exactly 194 Hero Versions and 970 base skill rows;
- requires exactly five slots per Hero Version;
- requires exactly 60 Awakening Skill rows;
- validates reviewed base rows have source technique, EN/VI names/descriptions, canon source/confidence and counterplay;
- rejects research debt in rows already marked `READY_DESIGN`;
- validates reviewed Awakening identity metadata;
- detects exact duplicate full technique kits among fully reviewed Hero Versions;
- prints unresolved base/Awakening review debt without pretending it is complete.

Production `--enforce` mode additionally requires:

- 970 / 970 base skill identities reviewed;
- 194 / 194 Hero Versions fully reviewed;
- 60 / 60 Awakening Skill identities reviewed;
- zero duplicate reviewed full kits.

## Status semantics

A generated candidate is not canon approval. A non-empty `canon_source` alone is not enough. Base skill identity is counted complete only when the final M50 row is explicitly `READY_DESIGN` and all required editorial fields pass validation.

An Awakening Skill is counted identity-reviewed only when its status is explicitly promoted to one of the final identity/design/runtime states and its EN/VI/canon metadata contains no unresolved-debt marker.

## CI / production integration

`.github/workflows/m58-skill-identity-integrity.yml` runs the truthful audit on skill/content changes.

`production-release-gate.yml` runs:

```bash
python scripts/validate-m58-skill-identity.py --enforce
```

so release cannot proceed while the full-roster identity review is incomplete.

## Development sequencing

The gate is integrated before all individual canon research is complete so M59/M60 mechanics and balance tooling can continue in parallel. This does **not** mean the M58 content DoD is satisfied; unresolved rows stay visible and remain a hard production blocker until researched and reviewed.

No canon source, technique selection, translation or Awakening identity is fabricated by this milestone.
