# M55 Android Playtest Build

M55 turns the Android build code into a reproducible artifact pipeline while preserving the physical-device release gate.

## Build targets

### Development APK

Pull requests that change Unity/mobile build files define a Development APK job using `game-ci/unity-builder@v5`.

- Unity version: read from `client-unity/ProjectSettings/ProjectVersion.txt`
- target: Android
- scripting backend: IL2CPP
- architecture: ARM64
- orientation: landscape
- output: `builds/android/NinjaAssemble.apk`
- metadata: `builds/android/build-metadata.json`
- artifact retention: 14 days
- build flags: `BuildOptions.Development | BuildOptions.StrictMode`

### Signed Release AAB

The Release AAB lane is manual (`workflow_dispatch`, `build_type=release|both`) and requires:

- `UNITY_LICENSE`
- `UNITY_EMAIL`
- `UNITY_PASSWORD`
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASS`
- `ANDROID_KEYALIAS_NAME`
- `ANDROID_KEYALIAS_PASS`

The keystore is materialized only in the ephemeral workspace. `MobileBuildAutomation` rejects release signing paths outside the Unity project workspace.

Expected output: `builds/android/NinjaAssemble.aab` plus build metadata.

## Build automation

`NinjaAssemble.EditorTools.MobileBuildAutomation` remains authoritative for:

- generated mobile scenes/build settings;
- Android application ID and product settings;
- IL2CPP + ARM64 configuration;
- landscape orientation;
- version and versionCode values from environment/command-line parameters;
- development-vs-release signing behavior;
- APK/AAB output paths;
- `build-metadata.json` generation.

## Build metadata

Every BuildPipeline attempt writes metadata containing:

- APK/AAB type;
- Unity version;
- application ID;
- bundle version and Android versionCode;
- git commit when available;
- output filename and byte size;
- build duration;
- scene count;
- UTC build timestamp;
- BuildPipeline result.

## Validation state

The M55 source/build contract has been reviewed against the current `game-ci/unity-builder@v5` action inputs and the repository validator checks the Android/IL2CPP/ARM64/signing/artifact contract.

GitHub Actions is currently failing before hosted-runner allocation (`steps=null`), and Unity credentials/signing secrets are not available to this execution environment. Therefore M55 integration means **build pipeline implemented**, not **APK/AAB artifact proven**.

Under section 6 of `docs/IMPLEMENTATION-MERGE-POLICY.md`, this non-certification pipeline milestone may merge after final diff/source-contract review while the external CI runner is unavailable. No unavailable build is represented as PASS.

## Physical device gate

A successful CI APK is not a physical-device PASS.

`game-data/release/mobile-device-evidence.csv` remains the release source of truth. Valid release certification still requires real rows with device model, Android version/class, smoke/performance results, FPS/frame-time/memory measurements and capture references tied to an exact built artifact SHA.

Do not generate placeholder PASS rows from CI or from the outage exception. M76/M77 retain hard artifact/device/release gates.
