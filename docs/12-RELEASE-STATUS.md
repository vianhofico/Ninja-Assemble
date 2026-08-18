# Mobile Release Status

Current implementation checkpoint: **M57 — realtime/Rage reference schema and evidence-gate hardening.**  
Current completion-governance checkpoint: **M56 — authoritative completion roadmap and merge policy.**

Merged foundations:

- **M54** — playable-quality battle presentation.
- **M55** — reproducible Android build pipeline implementation; real APK/AAB execution remains unverified while Actions/Unity credentials are unavailable.
- **M57** — canonical realtime/Rage measurement schemas and production evidence gates.

Immediate queue:

1. **M58** — full Hero Version skill identity/canon/editorial review.
2. **M59** — executable mechanics and realtime timing review.
3. **M60** — balance/presentation review.
4. Continue M61+ from the newest `main` per `docs/100-PERCENT-COMPLETION-PLAN.md`.

---

## Completion dashboard

| Area / gate | Current | Release target | State |
|---|---:|---:|---|
| Base/reference characters | 189 | >=180 | PASS foundation |
| Collectible Hero Versions | 194 | frozen release roster | PASS structure |
| Base skill slots | 970 (=194×5) | 100% final reviewed | STRUCTURE PASS / REVIEW BLOCKED |
| Awakening Skills | 60 | 100% final reviewed | STRUCTURE PASS / REVIEW BLOCKED |
| Playable/reference variant census | 427 | frozen release presentation census | PASS census |
| Production art packages fully READY | 0 / 427 | 427 / 427 | BLOCKED |
| Reference/balance profiles VERIFIED | 0 / 10 | 10 / 10 | SCHEMA PASS / EVIDENCE BLOCKED |
| Realtime timing corpus | schema defined, 0 measured rows | threshold-backed VERIFIED | EVIDENCE BLOCKED |
| Rage rules corpus | schema defined, 0 measured rows | threshold-backed VERIFIED | EVIDENCE BLOCKED |
| Battle presentation | M54 foundation merged | production hero-specific presentation | FOUNDATION PASS |
| Android build pipeline | M55 merged | reproducible artifact proof | IMPLEMENTED / EXECUTION UNVERIFIED |
| Passing Android device evidence | 0 | >=2 devices and >=2 classes | BLOCKED |
| Full production Campaign | vertical-slice content | frozen production census | BLOCKED |
| Resource PvE modes | foundations/partial | frozen production census | BLOCKED |
| Arena/Shadow combat | realtime foundations | complete seasonal/meta/UI loop | PARTIAL |
| Production mobile UI | functional shell/partial | full release navigation graph | BLOCKED |
| Full fresh-account -> late-game E2E | not complete | PASS | BLOCKED |
| `release-audit.py --enforce` | intentionally fails until evidence/art/device gates pass | PASS | BLOCKED |

`READY`, `VERIFIED`, `PARITY_PASS`, artifact PASS and device PASS are evidence states. They must never be inferred from implementation progress.

---

## M57 evidence truth

M57 removes stale turn/energy terminology from release measurement schemas:

- `energy_before/energy_after` -> `rage_before/rage_after`;
- `duration_turns` -> `duration_ms/tick_interval_ms`;
- dedicated `REALTIME_TIMING` corpus;
- dedicated `RAGE_RULES` corpus.

All 10 balance profiles remain `EXPERIMENTAL`, and the new corpora start header-only. Current VERIFIED count remains **0 / 10**.

The evidence validator now maps every release category to a concrete corpus/schema, rejects legacy fields and placeholder confidence debt in promoted evidence, and enforces sample/context/evidence-ref thresholds for VERIFIED profiles.

The production release workflow now runs both `validate-reference-evidence.py` and `release-audit.py --enforce`; false status promotion cannot satisfy the production gate by itself.

---

## Remaining release blockers

### Skill completion — M58–M60
Final identity/editorial, mechanics/timing, balance and presentation review for the full release roster.

### Gameplay/content — M61–M65
Complete Campaign, resource PvE, competitive, progression and economy/live-loop vertical slices.

### Production UI — M66–M68
Final mobile UX, state handling, safe-area/aspect and complete interactions.

### Production art — M69–M73
Real repository-backed portrait/icon/chibi/animation/VFX/SFX/regression packages; current fully READY count is 0 / 427.

### Evidence/parity — M74
Real measurements must satisfy all final profile thresholds; M57 only makes the gate enforceable.

### E2E/reliability — M75
Fresh-account -> late-game E2E plus persistence/migration/concurrency/idempotency regression proof.

### Device/release certification — M76–M77
Real artifacts, physical-device evidence, signing proof, performance and final release audit remain non-bypassable hard gates.

---

## Merge workflow

```text
latest main
 -> one milestone branch
 -> implementation + available validation
 -> final diff review
 -> check CI
 -> fix real failures
 -> if a non-release job cannot allocate a runner, document the outage and use policy §6 only when equivalent validation is genuine
 -> squash merge
 -> next milestone from new main
```

Release-certification evidence cannot use the outage exception.

---

## Roadmap

```text
M54 [MERGED]
 -> M55 [MERGED]
 -> M57 [INTEGRATION]
 -> M58-M60 skills
 -> M61-M65 gameplay/content
 -> M66-M68 mobile UI
 -> M69-M73 production art
 -> M74 parity/balance
 -> M75 E2E/reliability
 -> M76 device certification
 -> M77 release candidate
```

Desktop remains gated behind M77 mobile release certification.
