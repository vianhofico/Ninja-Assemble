# M17 — Playable Mobile Vertical Slice

## User flow

The first end-to-end playable flow is:

1. guest login;
2. idempotent starter bootstrap;
3. show owned roster while the catalog still exposes all 189 base characters / 427 variants;
4. save a five-ninja campaign formation;
5. run a server-seeded deterministic five-vs-five battle;
6. spend Energy and grant Gold on victory server-side;
7. perform seeded Complete Roster+ summons using Diamond, hard pity and duplicate Hero Coin conversion;
8. unlock pulled variants and select a variant for an owned character;
9. level up owned heroes with a versioned experimental Gold-cost profile.

Unity's `PlayableGameStore` connects these endpoints so UI scenes can bind to working game state instead of mock data.

## Starter team

The current private vertical-slice starter roster is Naruto, Sasuke, Sakura, Kakashi and Iruka. Bootstrap Gold/Diamond grants use immutable/idempotent wallet ledger keys, so reconnecting does not duplicate starter currency.

## Security / authority

- roster ownership is stored and checked server-side;
- formation only accepts owned unique heroes and exactly five slots;
- battle seed is generated server-side;
- battle simulation and reward decision are server-side;
- Energy is spent server-side;
- summon cost, pity state, result and duplicate conversion are server-side;
- summon and level-up request IDs are idempotent;
- client only renders returned state/timeline.

## Balance warning

The vertical slice intentionally uses versioned **experimental** balance profiles:

- `experimental-combat-stats-v1` derives stable temporary combat stats for every catalog character/form;
- `experimental-v1-unverified-formula` remains the battle damage ruleset;
- `complete-roster-experimental-v1` is the development summon pool/rate profile;
- `experimental-level-cost-v1` is the temporary hero level-up cost profile.

These are not claimed to be exact Ninja Assemble hidden formulas. They are replaceable without changing the ownership, formation, replay or mobile API contracts.

## Remaining mobile blockers

This vertical slice makes the data/gameplay path executable. Final mobile completion still requires the 427-variant art production gate plus full scene/prefab implementation and reference-value tuning.
