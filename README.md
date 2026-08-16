# Ninja Assemble — Private Clean-Room Replica

Private educational/research implementation that recreates the **observable gameplay systems** of Ninja Assemble / Ninja Rebirth from public gameplay evidence and user-supplied references.

> This repository does not contain ripped APK assets, proprietary source code, decompiled server logic, or redistributed copyrighted game resources. Character/art packages are kept external and plugged into the data-driven content layer.

## Current status

**Phase R0/M0 — research baseline + executable game-core bootstrap.**

Implemented in this bootstrap:

- researched game-rule specification with confidence labels;
- parity census for features and known roster evidence;
- Java 21 / Spring Boot 4.1 modular-monolith server skeleton;
- verified Arena and Shadow Arena roster/series rules;
- verified Frame progression model (Genin → Chunin → Jonin → Kage → Six Path → Awakening);
- PostgreSQL + Redis local stack;
- Flyway baseline schema for players, heroes, formations and wallet balances;
- Unity project shell and shared battle constants;
- CI and a dependency-free Java core smoke test.

## Repository layout

```text
.
├── client-unity/          # Unity client shell; chibi 2D presentation layer
├── server/                # Java 21 / Spring Boot game server
├── game-data/             # Data-driven content schemas + parity census
├── docs/                  # Research, rules, architecture and delivery plan
├── scripts/               # Local validation helpers
├── .github/workflows/     # CI
└── docker-compose.yml     # PostgreSQL + Redis
```

## Validate the verified core rules

Java 21 is enough for the first smoke test:

```bash
bash scripts/validate-core.sh
```

When Maven is available:

```bash
mvn -f server/pom.xml test
```

## Local infrastructure

```bash
docker compose up -d postgres redis
```

## Rule of implementation

No feature is considered parity-complete just because it “looks similar”. Every system moves through:

`DISCOVERED → DOCUMENTED → IMPLEMENTED → VERIFIED → PARITY_PASS`.

See [`docs/00-MASTER-PLAN.md`](docs/00-MASTER-PLAN.md) and [`docs/01-GAME-RULES.md`](docs/01-GAME-RULES.md).
