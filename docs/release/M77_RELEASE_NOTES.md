# Ninja Assemble — M77 Release Candidate Notes

This document describes the implemented mobile release baseline. It is **not** a statement that the game is certified for public distribution.

## Implemented baseline

- Realtime Rage-based 5v5 combat replay/presentation.
- 12-stage Campaign with multi-wave battles, first/repeat rewards and idempotent sweep.
- Nine Resource PvE modes with daily attempt/reset rules.
- Arena 5v5 and Shadow Arena 15-unit/3-squad competitive loops with defense, history, seasons and idempotent rewards.
- Hero ownership, level-up, Frame Advance, one-time Awakening, equipment and advanced Scroll/Ninja College/Tailed-Beast progression.
- Shops, summon/pity, quests, events, mail and guild loops.
- VI/EN mobile UX, production UI foundation, core/live screens and accessibility preferences.
- Android APK/AAB automation contract, performance profiles and physical-device evidence framework.
- Production HMAC player sessions, security headers and Redis-backed API rate limiting.

## Certification blockers

Public RC/tagging is blocked until M74 parity/reference evidence, M75 real E2E evidence, M76 physical Android evidence, 427/427 production art packages, signed AAB evidence, database restore evidence, store URLs/forms and licensing/IP provenance all pass M77 strict validation.

See `M77_KNOWN_ISSUES.md` and `M77_SUPPORT_RUNBOOK.md` for operational context.
