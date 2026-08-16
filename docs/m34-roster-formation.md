# M34 — Playable Roster, Hero Detail and Formation

## Runtime contract

- Ninja Roster cycles through authoritative `PlayableGameStore.Heroes` and shares the selected hero with Hero Detail and Formation.
- Hero Detail resolves variants from the server catalog, selects a variant through `SelectVariantAsync`, and trains the selected hero through `LevelUpAsync`.
- Formation keeps exactly five unique owned heroes and persists changes through `SaveFormationAsync`; the server revalidates ownership, team size and duplicate IDs.
- Client selection is presentation state only. Ownership, progression, variant state and formation membership remain server-authoritative.

## Regression invariants

`roster-formation-integrity` checks the actual server invariants (`BattleRules.requireArenaTeamSize`, duplicate rejection, ownership validation, `formation_slots` persistence) rather than milestone-era naming tokens.

The Guild regression gate accepts the domain's overflow-safe `Math.addExact(totalDamage, applied)` accumulation and the existing `damageIsCappedByRemainingBossHp` regression test.

## Scope

This milestone intentionally uses a compact one-button mobile interaction: cycle selected hero, cycle variant, train, and replace the fifth formation slot. A future drag/drop editor can reuse the same server contracts without changing game rules.
