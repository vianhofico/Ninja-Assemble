# Ninja Assemble — Private Clean-Room Expanded Replica

Private educational/research implementation that recreates the **observable gameplay systems** of Ninja Assemble / Ninja Rebirth and expands the playable roster across Naruto + Naruto Shippuden.

> This repository does not contain ripped APK assets, proprietary source code, decompiled server logic, or redistributed original game resources. Gameplay is reimplemented clean-room; production art is ingested through explicit file-backed package contracts.

## Current status

The project has moved well beyond bootstrap. Current mobile foundations include:

- **189 base characters / 427 playable variant census rows**;
- **120 bilingual EN/VI techniques**, 44 reusable kit profiles and complete base-character kit mapping;
- Java 21 / Spring Boot server with player state, wallet/energy, hero ownership, formation, deterministic battle/replay, progression/evolution, campaign, resource PvE, Arena/Shadow Arena, summon/pity, shops, inventory/equipment, guild, daily/events and mail;
- Unity mobile client shell with Bootstrap + 16 core screens and live battle/summon/level-up vertical-slice actions;
- data-driven Addressables presentation contracts;
- component-level art production gates for portrait/icon/chibi/animation/VFX/SFX/regression capture/review;
- evidence-backed parity gates for combat stats, damage formula, summon profile and level cost;
- Android APK/AAB build automation and release-device evidence contracts.

### Release blockers

The mobile game is **not declared finished yet**. The release audit intentionally remains blocked until:

1. all 427 required art packages are real and complete;
2. all 4 release-critical reference/balance profiles are verified from measurements;
3. Android builds pass smoke + performance runs on at least two distinct device models/classes.

See `docs/12-RELEASE-STATUS.md`.

## Repository layout

```text
.
├── client-unity/          # Unity 6000.0 mobile client + editor/build automation
├── server/                # Java 21 / Spring Boot game server
├── game-data/             # roster, variants, skills, localization, balance evidence
├── art/                   # manifests, package schema, regression/review contracts
├── docs/                  # rules, architecture, milestone and release documentation
├── scripts/               # validation, generation, release/build helpers
├── .github/workflows/     # server + content integrity CI
└── docker-compose.yml     # PostgreSQL + Redis
```

## Server validation

```bash
bash scripts/validate-core.sh
mvn -f server/pom.xml test
```

## Content / release validation

```bash
python scripts/validate-content.py
python scripts/validate-art-packages.py
python scripts/validate-production-assets.py
python scripts/validate-reference-evidence.py
python scripts/validate-unity-shell.py
python scripts/validate-mobile-build-source.py
python scripts/validate-mobile-release-evidence.py
python scripts/release-audit.py --markdown
```

Strict release checks intentionally fail while real art/reference/device evidence is incomplete.

## Local infrastructure

```bash
docker compose up -d postgres redis
```

## Quick local playtest

Start PostgreSQL + Redis, then launch the server with the playtest-only dev endpoints enabled:

```bash
docker compose up -d postgres redis
GAME_DEV_ENABLED=true mvn -f server/pom.xml spring-boot:run
```

PowerShell:

```powershell
$env:GAME_DEV_ENABLED="true"
mvn -f server/pom.xml spring-boot:run
```

Open `client-unity` in Unity 6000.0 and run:

`Ninja Assemble → Mobile → Play Development Game`

The command regenerates the mobile scene shell when needed, opens Bootstrap and enters Play Mode. A `DEV` button appears in the top-right corner in Unity Editor and development builds only. It provides:

- +1,000,000 Gold / +10,000 Diamond;
- unlock all base heroes and rebuild the first five-ninja formation;
- refill Energy;
- jump to Battle and Summon test screens;
- reset the local guest identity.

The backing `/api/v1/dev/**` endpoints do not exist unless `GAME_DEV_ENABLED=true`; keep this disabled outside local/playtest environments.

## Generate the Unity mobile scene shell

Open `client-unity` in Unity 6000.0 and run:

`Ninja Assemble → Mobile → Generate Complete Scene Shell`

This generates Bootstrap plus Home, Ninja Roster, Hero Detail, Formation, Adventure, Battle, Summon, Arena, Shadow Arena, Guild, Shop, Inventory, Quest, Events, Mail and Settings scenes.

## Android build

With Unity Android Build Support installed:

```bash
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh development
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh release
```

Development produces an APK; release produces an AAB. Build output is written under `builds/android/` and is ignored by Git.

## Automated playtest APK

`.github/workflows/playtest-apk.yml` builds a development APK after relevant changes land on `main`, and it can also be started manually with **Actions → Playtest APK → Run workflow**.

Before the first CI build, configure these repository Actions secrets for the Unity license used by GameCI:

- `UNITY_LICENSE`
- `UNITY_EMAIL`
- `UNITY_PASSWORD`

Successful runs upload `NinjaAssemble-playtest-<run number>` as a GitHub Actions artifact containing `NinjaAssemble.apk`. The artifact is retained for 14 days.

## Art production rule

A playable variant cannot become release-ready from a CSV status alone. Any component marked READY must be backed by a real descriptor under:

`art/packages/<character_id>/<variant-slug>/package.json`

and concrete repository files for that component. Final review READY also requires review evidence.

## Implementation rule

Observable Ninja Assemble parity and the expanded Naruto/Shippuden roster are audited separately. No feature or character is considered complete merely because it exists in a census.

See `docs/00-MASTER-PLAN.md`, `docs/12-RELEASE-STATUS.md`, `docs/16-M20-REFERENCE-EVIDENCE.md`, `docs/17-M19-ART-PACKAGE-GATES.md`, and `docs/18-M21-MOBILE-BUILD-ASSET-INGEST.md`.
