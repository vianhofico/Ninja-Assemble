# M19 — Component-Level Art Package Gates

## Purpose

A single `CONCEPT / IN_PROGRESS / REVIEW / READY` flag is too coarse for a 427-variant production queue. A hero could appear `READY` while still missing an animation set, VFX, SFX or regression capture. M19 makes every release package auditable at component level.

## Required package components

Every playable variant must independently complete these eight gates:

1. portrait
2. roster icon
3. 2–2.5-head chibi battle prefab
4. animation set
5. skill/ultimate VFX set
6. SFX/voice mapping set
7. visual-regression capture
8. final review

Allowed component states are:

`TODO -> CONCEPT -> IN_PROGRESS -> REVIEW -> READY`

The overall `hero-art-manifest.csv` status may be `READY` only when the matching component-status row exists and **all eight component gates are READY**.

## Flagship wave 1

The first tracked package wave contains 12 high-impact variants already represented in the art manifest:

- Naruto — Sage Mode
- Naruto — KCM2
- Naruto — Six Paths Sage Mode
- Sasuke — Eternal Mangekyo Sharingan
- Sasuke — Rinnegan
- Itachi — Susanoo
- Madara — Ten-Tails Jinchuriki
- Obito — Ten-Tails Jinchuriki
- Kakashi — Double Mangekyo
- Might Guy — Eighth Gate
- Gaara — Kazekage
- Minato — KCM

Their portrait/icon work remains `CONCEPT`; combat-prefab/animation/VFX/SFX/capture/review gates remain `TODO`. Nothing is promoted to READY by M19.

## Files

- `art/manifests/hero-art-component-status.csv` — human-reviewed component progress.
- `scripts/validate-art-packages.py` — validates state transitions and prevents false overall READY.
- `scripts/generate-full-art-component-status.py` — produces a complete 427-row TODO candidate while preserving tracked rows.
- `scripts/art-production-queue.py` — prioritizes flagship/reference rows and reports `components_ready / 8` plus completion percentage.

## Development vs release validation

Development CI allows untracked/TODO packages while enforcing consistency for any row that exists.

Release validation is intentionally strict:

- every census variant needs a component package;
- every one of the eight component gates must be READY;
- every overall art manifest row must also be READY with complete runtime Addressables.

## Completion condition

M19 engineering is complete when CI enforces the package model and can generate a 427-row component candidate. **Art production itself is not complete** until the real queue reaches `427 / 427` packages with all eight gates READY.
