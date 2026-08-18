# M76 — Android Performance and Physical-Device Certification

M76 defines measurable Android runtime targets and the evidence format required before a release candidate may be certified. The milestone commits the framework only; it does not create physical-device PASS evidence.

## Benchmark scenario

The canonical scenario is `campaign-realtime-battle-rage-v1`. A physical-device run warms up for 30 seconds and then measures at least 180 seconds. `MobilePerformanceProbe` records unscaled frame time, average FPS, p95 frame time and peak Unity allocated memory, and writes a local JSON result under `Application.persistentDataPath`.

The probe is diagnostic only. It cannot alter release or gameplay state and cannot promote an evidence row.

## Device classes

`game-data/release/m76-device-profiles.json` freezes three engineering targets:

- LOW: ≥30 average FPS, ≤40 ms p95, ≤1400 MB.
- MID: ≥45 average FPS, ≤28 ms p95, ≤1800 MB.
- HIGH: ≥55 average FPS, ≤22 ms p95, ≤2200 MB.

These are product engineering thresholds, not claims about third-party/reference parity.

## Physical evidence

`game-data/release/mobile-device-evidence.csv` is the certification ledger. Each passing row must identify the exact commit, Unity version, APK/AAB artifact, physical device model, Android version, device class, build fingerprint, benchmark profile and capture reference. Emulator rows are not accepted in this ledger.

Release certification requires at least two passing physical Android device models across at least two LOW/MID/HIGH classes, all matching the exact release commit and their class-specific thresholds.

## Commands

Framework validation:

`python scripts/validate-m76-android-performance.py`

Release certification:

`python scripts/validate-m76-android-performance.py --enforce`

The strict command is expected to remain red while the physical-device evidence ledger is empty.
