# Art and Device Completion Runbook

This runbook closes the two production-release blockers reported by `scripts/validate-release-readiness.py` without weakening validation or inventing evidence.

Current baseline at introduction of this runbook:

- variant census: **427**;
- tracked component-status rows: **12**;
- fully READY art packages: **0**;
- Android device evidence rows: **0**.

The playable code baseline can remain green while these production tasks are incomplete. The manual `production-release-gate` must stay blocked until the evidence below exists.

## A. Complete the 427-variant production art backlog

### A1. Generate complete tracking candidates

Use the existing generators rather than hand-maintaining a partial census:

```bash
python scripts/generate-full-art-manifest.py --output /tmp/hero-art-manifest-full.csv
python scripts/generate-full-art-component-status.py --output /tmp/hero-art-components-full.csv
```

Review the generated rows, then intentionally merge them into the canonical art tracking files. Never bulk-mark them READY.

### A2. Package convention

Each variant that advances into production uses:

```text
art/packages/<character_id>/<variant-slug>/package.json
```

A production package should point to concrete repository files for the components it claims READY:

- portrait;
- icon;
- chibi prefab;
- animation set;
- VFX set;
- SFX set;
- regression capture;
- review evidence.

The descriptor identity must match `characterId` + exact variant string from the census.

### A3. Status lifecycle

Use the existing states honestly:

```text
TODO → CONCEPT → IN_PROGRESS → REVIEW → READY
```

A recommended per-variant workflow:

1. **CONCEPT** — approved chibi silhouette, costume/form identifiers, palette and distinguishing props/effects.
2. **IN_PROGRESS** — portrait/icon/chibi implementation and required battle animations/VFX/SFX underway.
3. **REVIEW** — package is integrated in Unity and regression captures exist for roster/detail/battle.
4. **READY** — every required component is concrete, package paths resolve, review evidence is attached, and no placeholder/concept is being presented as final.

Do not infer READY from attractive generated concept art alone. READY means integrated production files and review evidence.

### A4. Batch strategy

Complete art in waves so gameplay can be reviewed continuously:

- **Wave A — flagship forms:** finish the 12 currently tracked high-visibility variants first.
- **Wave B — starter/common roster:** base/early variants most likely to appear in onboarding, summon and campaign.
- **Wave C — major progression chains:** Naruto, Sasuke, Akatsuki, Kage, jinchuriki, war-arc/high-tier transformations.
- **Wave D — remaining census:** finish every remaining canonical variant.

Every wave should keep identifiers compatible with the existing 427-variant census, not introduce an independent visual roster.

### A5. Validation during art production

Run on every art PR:

```bash
python scripts/validate-content.py
python scripts/validate-art-packages.py
python scripts/validate-production-assets.py
python scripts/validate-reference-evidence.py
python scripts/generate-unity-art-runtime-catalog.py --output /tmp/hero-art-runtime-catalog.json
```

For a release candidate, additionally require:

```bash
python scripts/validate-art-packages.py --release
python scripts/validate-production-assets.py
```

Expected final condition:

- component-status keys = all **427** census keys;
- fully READY packages = **427/427**;
- every READY file/review reference resolves inside the repository.

## B. Produce Android build/device evidence

### B1. Build a real artifact

Use the current Unity Android build automation from the exact release candidate SHA. Record:

- Git SHA;
- Unity version;
- artifact type (`APK` or `AAB` plus installable APK where needed for testing);
- artifact reference/location.

Do not put a guessed or nonexistent artifact reference into the evidence CSV.

### B2. Minimum device matrix

Production release requires at least:

- **2 passing evidence rows**;
- **2 distinct Android device models**;
- **2 distinct classes** from `LOW`, `MID`, `HIGH`.

A better target is three devices covering LOW/MID/HIGH when available.

Physical devices are preferred for release evidence. An emulator may be useful during development, but any release policy decision should clearly label emulator evidence rather than presenting it as physical-device testing.

### B3. Smoke flow

At minimum, exercise the complete playable baseline:

1. guest login/bootstrap;
2. Home/resource state;
3. Roster selection + Hero Detail;
4. Formation save;
5. Campaign normal + boss multi-wave battle/replay;
6. Summon;
7. Arena;
8. Shadow Arena eligibility/series where the test account has 15 ninja;
9. Guild create/join/contribution/boss;
10. Shop purchase;
11. Inventory/equipment equip + enhance and subsequent combat;
12. Daily Quest claim;
13. Weekly Event claim;
14. Mail claim;
15. Frame Advance/Evolution with an eligible hero;
16. EN/VI switch;
17. audio/graphics settings persistence;
18. app restart/account state persistence.

A smoke pass must represent an actual run of the release artifact on the recorded device.

### B4. Performance capture

For each passing row record real positive values for:

- `avg_fps`;
- `p95_frame_ms`;
- `max_memory_mb`.

Capture the heaviest representative scenes, particularly multi-wave battle, Shadow Arena series and repeated roster/inventory navigation.

Use a reproducible capture method and put its evidence reference in `capture_ref`.

### B5. Fill the canonical evidence file

Update:

```text
game-data/release/mobile-device-evidence.csv
```

Every row must include:

- evidence ID;
- Git SHA;
- Unity version;
- artifact type/reference;
- device model;
- Android version;
- LOW/MID/HIGH class;
- smoke pass;
- performance pass;
- average FPS;
- p95 frame time;
- maximum memory;
- capture reference;
- useful notes.

Never add synthetic rows just to satisfy the validator.

### B6. Validate device evidence

Development check:

```bash
python scripts/validate-mobile-release-evidence.py
```

Release check:

```bash
python scripts/validate-mobile-release-evidence.py --release
```

## C. Final production release sequence

Only after Art A and Device B are complete:

1. Freeze the candidate Git SHA.
2. Run all normal PR/main workflows.
3. Run locally or in CI:

```bash
python scripts/validate-release-readiness.py --release
python scripts/validate-art-packages.py --release
python scripts/validate-production-assets.py
python scripts/validate-mobile-release-evidence.py --release
mvn -B -f server/pom.xml test
```

4. Trigger `.github/workflows/production-release-gate.yml` manually on the candidate branch/SHA.
5. Do not tag/publish if any gate fails.
6. Archive the exact artifact and evidence references used by the passing release gate.

## D. What cannot be replaced by code automation

The following require real production work/evidence and must not be simulated by scripts:

- final approved chibi/portrait/icon/animation/VFX/SFX packages for every released variant;
- human visual review evidence;
- actual build artifact generation in a valid Unity build environment;
- smoke/performance measurements on the recorded Android devices;
- distribution signing credentials/store submission decisions.

Automation should verify those facts; it should not manufacture them.
