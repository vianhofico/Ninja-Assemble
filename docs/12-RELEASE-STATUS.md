# Mobile Release Status

Current implementation checkpoint: **M59 — full-roster realtime mechanics/timing audit gate.**

Merged implementation foundations: M54 battle presentation, M55 Android build pipeline, M57 evidence schemas, M58 identity/editorial gate, and M59 canonical mechanics review gate.

Immediate queue: **M60 balance/presentation → M61 Campaign → M62 Resource PvE → M63 competitive → M64 progression → M65 economy/live → M66–M68 production UI → M69+ art/parity/release**.

## Release dashboard

| Gate | Current | Target | State |
|---|---:|---:|---|
| Hero Versions | 194 | 194 | STRUCTURE PASS |
| Base skill identity | partial explicit READY_DESIGN | 970 / 970 | BLOCKED |
| Awakening identity | structural 60 | 60 / 60 reviewed | BLOCKED |
| Base mechanics review registry | 0 / 970 | 970 / 970 | BLOCKED |
| Awakening mechanics review registry | 0 / 60 | 60 / 60 | BLOCKED |
| Reference profiles VERIFIED | 0 / 10 | 10 / 10 | BLOCKED |
| Production art fully READY | 0 / 427 | 427 / 427 | BLOCKED |
| Android build pipeline | implemented | real artifact proof | EXECUTION UNVERIFIED |
| Passing physical Android evidence | 0 | >=2 devices / >=2 classes | BLOCKED |

Generated mechanics defaults are executable-development inputs, not review evidence. `m59-mechanics-reviews.csv` starts header-only and production `--enforce` requires all 1030 skill mechanics reviews.

## M59 contract

- Rage-only resource semantics.
- cooldown/cast/impact/recovery measured in milliseconds.
- no turn/round triggers.
- explicit target/effect profiles.
- Basic generates Rage.
- Skill 1 is the 100-Rage signature skill.
- Skill 2/3 have positive realtime cooldowns.
- passive mechanics are event/time based.
- technique effects retain `duration_ms/tick_interval_ms` and explicit runtime/deferred mapping states.

## Hard release blockers

Actual M58 canon/editorial content, M59 mechanics evidence, M60 balance/presentation review, 0/10 verified reference profiles, 0/427 production art packages, full gameplay/UI/E2E completion, and real Android artifact/device/release proof remain unresolved. No later milestone may reinterpret these counts as PASS.

## Merge workflow

`latest main -> milestone branch -> implementation/audit -> diff -> CI check -> fix real failures -> documented non-release runner-outage exception when applicable -> squash merge -> next milestone`

Release evidence/certification cannot use the outage exception.
