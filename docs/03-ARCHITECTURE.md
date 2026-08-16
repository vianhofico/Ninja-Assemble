# Architecture

## High-level

```text
Unity Client
    |
    | HTTPS / WebSocket (only where real-time state is justified)
    v
Spring Boot Modular Monolith
    |
    +-- player
    +-- hero
    +-- progression
    +-- scroll
    +-- tailedbeast
    +-- formation
    +-- battle
    +-- campaign
    +-- pve
    +-- arena
    +-- shadowarena
    +-- guild
    +-- economy
    +-- shop
    +-- quest
    +-- event
    +-- mail
    |
    +--> PostgreSQL
    +--> Redis
```

## Why modular monolith first

The game systems are highly transactional and share player state. A modular monolith avoids premature network boundaries while preserving clear packages that can later be extracted if needed.

## Battle architecture

Battle is designed as a deterministic pure domain engine:

```text
BattleStartRequest
  + initial formations
  + hero snapshots
  + ruleset version
  + random seed
        |
        v
Deterministic Battle Engine
        |
        v
BattleTimeline
  - action events
  - damage/heal events
  - status events
  - energy events
  - death/revive events
  - round/match end
        |
        +--> persisted result/proof
        +--> Unity animation playback
```

The server owns the result. Unity renders the event stream.

## Ruleset versioning

Every battle stores a `ruleset_version` so future tuning does not invalidate old replays.

Example:

```text
na-ref-2026-08-ruleset-001
```

## Data-driven content

Static content is represented by versioned game-data definitions. Runtime ownership references definition IDs rather than embedding skill logic into database rows.

```text
HeroDefinition
  -> SkillDefinition[]
  -> EvolutionDefinition
  -> ScrollAffinity
  -> PresentationDefinition
```

## Content/IP boundary

The repository contains code, schemas, manifests and original placeholder resources only. User-owned/licensed/private reference art can be imported into Unity through asset-address keys without being committed to the public/content-neutral code layer.
