# M59 Realtime Mechanics and Timing Gate

M59 separates executable defaults from evidence-backed mechanics review.

## Canonical mechanics contract

Every normal Hero Version still resolves to exactly five M50 slots. The M59 audit requires their production candidate to use:

- Rage rather than Energy;
- millisecond cooldown/cast/impact/recovery values;
- realtime/event triggers, never turn/round triggers;
- explicit target selectors;
- an effect/technique profile from the canonical technique catalog;
- BASIC Rage generation;
- `SKILL_1 = RAGE_SKILL / RAGE_FULL / 100 Rage`;
- positive cooldowns for Skill 2 and Skill 3;
- event/time passive semantics.

The existing technique-effect coverage validator is also executed so structured effects remain millisecond-based and the server resolver cannot derive gameplay from prose/turn fields.

## Mechanics review registry

`game-data/skills/m59-mechanics-reviews.csv` is the explicit evidence registry for mechanics approval. It begins header-only: generated defaults are not silently called reviewed.

A `REVIEWED` row records the exact current mechanics contract for one skill plus an evidence reference. Base reviews must exactly match the generated production candidate so stale approvals fail after a mechanics change. Awakening reviews define the canonical realtime/Rage mechanics for the sixth skill and must reference a known technique/effect profile.

## Production enforcement

Normal audit mode validates structural executability and reports review counts.

`python scripts/validate-m59-mechanics.py --enforce` requires:

- 970 / 970 reviewed base-skill mechanics records;
- 60 / 60 reviewed Awakening mechanics records.

The production release workflow now runs this enforce gate after identity review.

## Truthful state

The registry is intentionally empty at integration time. Existing M50 defaults make the development battle pipeline executable, but they are not treated as final mechanics evidence. Legacy Awakening fields such as `chakra_cost/cooldown` remain migration debt until explicit M59 review records define canonical Rage/millisecond behavior.

## Next

M60 adds balance/presentation review and simulation/outlier gates. Identity and mechanics review debt remains visible and release-blocking while later implementation work continues.
