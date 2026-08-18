# Mobile Release Status

Current implementation checkpoint: **M58 — full-roster skill identity/editorial audit gate.**

Merged implementation foundations:

- **M54** playable-quality battle presentation.
- **M55** Android build pipeline implementation (artifact execution still externally unverified).
- **M57** canonical realtime/Rage reference schemas and evidence gates.
- **M58** full-roster skill identity/canon/editorial audit + production enforcement gate.

Immediate queue: **M59 mechanics/realtime timing → M60 balance/presentation → M61+ gameplay/content** from the newest `main`.

---

## Completion dashboard

| Area / gate | Current | Release target | State |
|---|---:|---:|---|
| Hero Versions | 194 | 194 | STRUCTURE PASS |
| Base skill slots | 970 | 970 reviewed | STRUCTURE PASS / IDENTITY REVIEW BLOCKED |
| Awakening Skills | 60 | 60 reviewed | STRUCTURE PASS / IDENTITY REVIEW BLOCKED |
| Reference/balance profiles VERIFIED | 0 / 10 | 10 / 10 | SCHEMA PASS / EVIDENCE BLOCKED |
| Production art packages fully READY | 0 / 427 | 427 / 427 | BLOCKED |
| Battle presentation | M54 merged | production hero-specific presentation | FOUNDATION PASS |
| Android build pipeline | M55 merged | real reproducible artifact proof | IMPLEMENTED / EXECUTION UNVERIFIED |
| Passing physical Android device evidence | 0 | >=2 devices / >=2 classes | BLOCKED |
| Production gameplay/content | foundations/partial | full release vertical slices | BLOCKED |
| Production mobile UI | functional shell/partial | full release navigation graph | BLOCKED |
| Full fresh-account -> late-game E2E | not complete | PASS | BLOCKED |

`READY_DESIGN`, `READY`, `VERIFIED`, `PARITY_PASS`, artifact PASS and device PASS are evidence/review states, never aliases for “code exists”.

---

## M58 identity truth

The repository structurally covers 194 Hero Versions × 5 normal skills plus 60 sixth Awakening Skills, but many entries are deliberate M47/M50 design seeds rather than final canon review.

`validate-m58-skill-identity.py` now audits the entire generated catalog and production `--enforce` requires:

- 970 / 970 reviewed base identities;
- 194 / 194 fully reviewed Hero Versions;
- 60 / 60 reviewed Awakening identities;
- zero exact duplicate full kits among reviewed versions;
- complete EN/VI/canon/editorial fields without research-debt markers.

Existing `RESEARCH_REQUIRED`, `UNREVIEWED`, `UNRESOLVED_EXPLICIT_DESIGN`, and equivalent debt remains visible and blocks release. M58 integration does not mass-promote structural candidates.

---

## Remaining release blockers

1. **M58 content debt:** complete actual canon/editorial review for every unresolved base and Awakening identity.
2. **M59–M60:** explicit deterministic mechanics, realtime timing, balance and presentation review.
3. **M61–M65:** full Campaign/PvE/PvP/progression/economy/live vertical slices.
4. **M66–M68:** production mobile UX.
5. **M69–M73:** real production art/animation/VFX/SFX packages; current fully READY = 0/427.
6. **M74:** real parity/balance evidence; current VERIFIED = 0/10.
7. **M75:** full E2E/reliability evidence.
8. **M76–M77:** real Android artifact/device/performance/signing/release proof.

---

## Merge workflow

```text
latest main -> milestone branch -> implementation/audit -> final diff -> check CI
 -> fix real failures
 -> non-release runner outage: document + policy §6 exception when equivalent validation is genuine
 -> squash merge -> next milestone from new main
```

Production release evidence gates cannot use the outage exception.
