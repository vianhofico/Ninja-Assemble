# M21 — Android Build & Production Asset Ingest

## Goal

Make the mobile client reproducibly buildable from Unity and make every future `READY` art claim traceable to concrete files and review evidence.

## Android build automation

`client-unity/Assets/Editor/MobileBuildAutomation.cs` provides:

- **Ninja Assemble → Mobile → Build Android Development APK**
- **Ninja Assemble → Mobile → Build Android Release AAB**

Before building it regenerates the M18 mobile scene shell and applies the Android player contract:

- application id `com.vianhofico.ninjaassemble`
- IL2CPP scripting backend
- ARM64 target architecture
- landscape orientation
- strict build mode
- APK for development / AAB for release

Optional environment variables:

- `NINJA_BUILD_VERSION`
- `NINJA_ANDROID_VERSION_CODE`

CLI helper:

```bash
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh development
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh release
```

The Unity editor must include Android Build Support and be launched with the Android target in batch mode.

## Production art ingest

M19 tracks component status. M21 adds the file-backed package contract:

`art/packages/<character_id>/<variant-slug>/package.json`

Schema: `art/hero-art-package.schema.json`.

A descriptor points to repo-relative files for:

- portrait
- icon
- chibi prefab
- animation set
- VFX set
- SFX set
- regression capture
- review evidence

`validate-production-assets.py` is intentionally asymmetric:

- TODO/CONCEPT/IN_PROGRESS work may exist without a descriptor;
- any component promoted to READY must have a package descriptor;
- every READY component path must resolve inside the repository and point to a real file;
- final review READY requires at least one real review-evidence file.

This prevents a CSV-only “READY” state from bypassing production work.

## Mobile build evidence

`game-data/release/mobile-device-evidence.csv` starts header-only. Do not seed fake passes.

Each real device run records:

- git SHA + Unity version
- APK/AAB artifact reference
- device model / Android version / device class
- smoke result
- performance result
- average FPS / p95 frame time / max memory
- capture/evidence reference

Release requires at least two passing rows on two distinct device models and at least two LOW/MID/HIGH device classes.

## CI

`content-integrity` now validates:

- content/roster/localization
- component-level art status
- actual files behind READY art components
- reference/balance evidence
- mobile scene source shell
- Android build automation source contract
- recorded device evidence consistency
- generated 427-row art + component candidates

CI does **not** pretend to build Unity without an installed/licensed Unity editor. The actual build command is deterministic and can be run locally or on a licensed Unity runner.

## Completion condition

M21 engineering is complete when the build/ingest/evidence contracts pass CI. Mobile release remains blocked until:

- 427/427 art packages are actually complete;
- 4/4 reference profiles are evidence-verified;
- real Android builds are produced and device evidence meets the release threshold.
