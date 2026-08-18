# Mobile Release Status

Current implementation checkpoint: **M60 — full-roster balance/presentation review gate.**

Merged foundations: M54 battle presentation, M55 Android pipeline, M57 evidence schemas, M58 identity gate, M59 realtime mechanics gate, M60 balance/presentation gate.

Immediate queue: **M61 Campaign → M62 Resource PvE → M63 Arena/Shadow Arena → M64 Progression → M65 Economy/Live → M66–M68 production UI → M69–M77 art/parity/E2E/device/release**.

## Release truth

| Gate | Current | Target | State |
|---|---:|---:|---|
| Hero Versions | 194 | 194 | STRUCTURE PASS |
| Base identity review | partial | 970 / 970 | BLOCKED |
| Awakening identity review | partial/structural | 60 / 60 | BLOCKED |
| Mechanics reviews | 0 / 1030 | 1030 / 1030 | BLOCKED |
| Balance/presentation reviews | 0 / 1030 | 1030 / 1030 | BLOCKED |
| Reference profiles VERIFIED | 0 / 10 | 10 / 10 | BLOCKED |
| Production art fully READY | 0 / 427 | 427 / 427 | BLOCKED |
| Physical Android device PASS | 0 | >=2 models/classes | BLOCKED |

M60 static sanity passes only structural/range/presentation-contract checks. It does not convert experimental values into balanced values. The M60 review registry starts empty and production enforcement requires a real committed simulation artifact for every approved review.

## Hard blockers preserved

Actual M58 canon review, M59 mechanics review evidence, M60 simulations/reviews, all reference verification, production art, complete gameplay/UI/E2E, and real Android release evidence remain hard release blockers. Later implementation milestones may proceed but cannot reinterpret these as PASS.

Workflow: `latest main -> milestone branch -> implementation/audit -> diff -> CI check -> fix real failures -> documented non-release runner-outage exception where valid -> squash merge -> next milestone`. Release certification cannot use the outage exception.
