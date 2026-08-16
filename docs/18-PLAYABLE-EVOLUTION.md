# M21 — Playable Frame Advance & Evolution

## Frame Advance

The playable application flow now supports the verified early frame structure:

- Genin: 1 advance → Chunin
- Chunin: 2 advances → Jonin
- Jonin: 3 advances → Kage
- Kage: 4 advances → Six Path

Each action is server-authoritative, charges Gold through the immutable wallet ledger and uses an idempotent request ID. Gold cost is currently a versioned experimental private-game profile (`frame-playable-experimental-v1`).

Six Path → Awakening remains blocked in this flow until the target reference requirements are measured or a separately-labelled expansion profile is approved. The code intentionally refuses to silently invent that transition.

## Playable form evolution

`playable-evolution-paths.csv` provides a first set of flagship transformation chains, including:

- Naruto: Sage Mode → KCM1 → KCM2 → Six Paths Sage Mode
- Sasuke: Mangekyo → Eternal Mangekyo → Rinnegan
- Madara / Obito: Rinnegan / White Mask → Ten-Tails Jinchuriki
- Guy: Seventh Gate → Eighth Gate
- Kabuto: Snake Sage
- Minato: KCM
- Hashirama: Sage Mode
- Orochimaru: White Snake
- Kisame: Samehada Fusion

These rows are explicitly marked `EXPANSION_PLAYABLE_PROFILE`. They make progression playable now but are not presented as exact Ninja Assemble hidden requirements.

Evolution validates:

1. owned hero identity;
2. minimum hero level;
3. minimum Frame tier;
4. prerequisite form unlock;
5. Gold balance;
6. idempotent request key;
7. target variant existence through the existing ownership/variant catalog.

On success the target variant is unlocked and immediately selected as the hero's current battle form.

## API

```text
GET  /api/v1/play/{playerId}/progression/evolution-paths/{characterId}
POST /api/v1/play/{playerId}/progression/heroes/{playerHeroId}/frame-advance
POST /api/v1/play/{playerId}/progression/heroes/{playerHeroId}/evolve
```

The next client step is to bind these endpoints into Hero Detail / Evolution UI and add visual evolution sequences once the matching chibi art packages reach REVIEW/READY.
