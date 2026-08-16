# Release Readiness

This repository separates **playable baseline completeness** from **production release readiness**. A green development CI run does not claim that the full Naruto-inspired art package or physical-device release evidence exists.

## Playable baseline

The mobile vertical slice now has live behavior for all 16 `ScreenId` destinations:

- Home — authoritative resource/progression summary.
- Ninja Roster — owned-ninja selection.
- Hero Detail — variant selection, training, Frame Advance and Evolution.
- Formation — authoritative five-ninja save.
- Adventure — data-driven campaign with multi-wave boss stages.
- Battle — deterministic 5v5 battle replay.
- Summon — server-authoritative summon/pity/duplicate handling.
- Arena — asynchronous 5v5 rating battle plus non-rewarding training fallback.
- Shadow Arena — gated 15-ninja, 3-squad best-of-three battle.
- Guild — create/join, contribution and daily guild boss.
- Shop — server-authoritative purchases and limits.
- Inventory — stack inventory plus equip/enhance gear that affects combat stats.
- Quest — server-audited daily objectives and idempotent claims.
- Events — server-audited weekly objectives and claims.
- Mail — persistent inbox and idempotent attachment claims.
- Settings — persistent EN/VI, audio and graphics preferences.

Runtime EN/VI localization consumes the packaged localization catalog and translates dynamic TMP text while preserving identifiers, proper names and live numeric values.

`python scripts/validate-release-readiness.py` is the normal CI gate for this level of completeness.

## Production release blockers

### Hero art packages

The canonical variant census is much larger than the currently tracked production-art packages. At the time this readiness document was introduced, `art/manifests/hero-art-component-status.csv` tracked 12 flagship variants and **0** packages had all of portrait, icon, chibi prefab, animation, VFX, SFX, regression capture and review marked `READY`.

Production release requires every census variant to have a complete READY package. `python scripts/validate-art-packages.py --release` enforces this and `validate-production-assets.py` verifies files behind READY claims.

### Android device evidence

`game-data/release/mobile-device-evidence.csv` currently contains only the header and no physical/emulated-device evidence rows. Production release requires at least two passing Android device rows, two distinct device models and two device classes among LOW/MID/HIGH, with smoke + performance metrics and capture references.

`python scripts/validate-mobile-release-evidence.py --release` enforces this.

## Release workflow

`.github/workflows/production-release-gate.yml` is manual (`workflow_dispatch`). It runs:

1. server core validation + Maven tests;
2. full `validate-release-readiness.py --release`;
3. full art package release validation;
4. production asset file validation;
5. Android device release-evidence validation.

The workflow is expected to fail until real art production and device testing are completed. Do not weaken the gate or insert synthetic evidence to make it green.

## Truth labels

Values inferred or invented to make the expanded playable build coherent remain explicitly tagged with profiles such as `DESIGN_BASELINE`, `EXPANSION_PLAYABLE_PROFILE`, or experimental version strings. Verified/researched rules and production evidence must remain distinguishable from those expansion choices.
