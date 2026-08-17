# M55 Android Playtest Build

M55 turns the existing Android build code into a reproducible CI artifact lane while preserving the physical-device release gate.

## Build targets

### Development APK

Every pull request that changes Unity/mobile build files runs the Development APK job.

- Unity version: read from `client-unity/ProjectSettings/ProjectVersion.txt`
- target: Android
- scripting backend: IL2CPP
- architecture: ARM64
- orientation: landscape
- output: `builds/android/NinjaAssemble.apk`
- metadata: `builds/android/build-metadata.json`
- artifact retention: 14 days

The APK is for internal playtesting and is explicitly built with `BuildOptions.Development | BuildOptions.StrictMode`.

### Signed Release AAB

The Release AAB job only runs from manual `workflow_dispatch` with `build_type=release` or `both`.

It requires these repository Actions secrets:

- `UNITY_LICENSE`
- `UNITY_EMAIL`
- `UNITY_PASSWORD`
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASS`
- `ANDROID_KEYALIAS_NAME`
- `ANDROID_KEYALIAS_PASS`

The keystore is materialized only in the ephemeral Actions workspace and is not committed. `MobileBuildAutomation` rejects release signing paths outside the Unity project workspace.

Output: `builds/android/NinjaAssemble.aab` plus build metadata.

## Local build

With a Unity 6000.0 editor executable that has Android Build Support installed:

```bash
UNITY_PATH=/path/to/Unity ./scripts/build-mobile.sh development
```

Release builds additionally require the signing variables accepted by `MobileBuildAutomation`.

## Build metadata

Every BuildPipeline attempt writes `build-metadata.json` with:

- APK/AAB type
- Unity version
- application ID
- bundle version and Android versionCode
- git commit when available
- output filename and byte size
- build duration
- scene count
- UTC build timestamp
- BuildPipeline result

## Physical device gate

A successful CI APK is **not** a passing Android device test.

`game-data/release/mobile-device-evidence.csv` remains the release source of truth. A valid release still needs real rows with device model, Android version/class, smoke result, performance result, FPS/frame-time/memory measurements and capture references.

Do not generate placeholder PASS rows from CI. Device evidence must reference the exact APK/AAB build commit/artifact that was installed and exercised.

## GameCI

The workflow uses `game-ci/unity-builder@v5`, with the repository's own `NinjaAssemble.EditorTools.MobileBuildAutomation` method remaining authoritative for scene generation, player settings and output paths.
