# Parity Census Workflow

The project must never equate “implemented something similar” with “cloned the system”. This document defines the evidence workflow.

## State machine

```text
DISCOVERED
  ↓
DOCUMENTED
  ↓
IMPLEMENTED
  ↓
VERIFIED
  ↓
PARITY_PASS
```

A row may move backwards when new reference evidence contradicts the current implementation.

## Evidence package per feature

Store locally (do not commit copyrighted source assets unless licensed):

```text
reference-local/
  <feature-id>/
    notes.md
    screenshots/
    video-clips/
    measurements.csv
```

The git-tracked census stores only metadata/checksums/notes.

## Hero census required fields

- canonical character;
- in-game variant/display name;
- availability/source;
- combat archetype;
- position/front/rear if present;
- base stats at a known level/frame;
- five skill slots if present;
- transformation eligibility;
- frame requirements;
- Tailed Beast state requirements;
- scroll affinity;
- portrait/model/animation/VFX reference IDs;
- confidence/evidence;
- parity status.

## Skill census required fields

- skill slot;
- active/passive;
- trigger;
- energy cost/gain;
- target selector;
- damage channel;
- coefficients;
- status effects;
- duration;
- upgrade breakpoints;
- Six Path/Awakening changes;
- animation timeline;
- VFX/SFX events.

## Screen census required fields

- source scene/menu;
- navigation entry condition;
- required player level/VIP/mode unlock;
- all interactive elements;
- red-dot rules;
- server data requirements;
- loading/error/empty states;
- screenshot at reference resolution;
- transition/animation timings.

## Release gate

The release audit fails if any reference-build feature is:

- absent from census;
- still `UNKNOWN` for implementation-critical rules;
- implemented without evidence;
- visually/functionally outside approved tolerance;
- missing a regression test where deterministic testing is possible.
