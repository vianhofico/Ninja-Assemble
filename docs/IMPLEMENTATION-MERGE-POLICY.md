# Implementation and Merge Policy

This document defines how every remaining Ninja Assemble milestone is implemented, validated and merged.

## 1. Core rule

Every completed stage must land in `main` before the next independent stage begins.

Default flow:

```text
latest main
 -> agent/mXX-short-description
 -> implementation
 -> tests/validators
 -> PR to main
 -> review/CI
 -> merge
 -> latest main
 -> next milestone branch
```

Do not build a long chain of milestone branches where M+1 is permanently based on unmerged M. The M47-M52 stacked-history problem must not be repeated.

A temporary child branch is acceptable only when work truly cannot wait for its parent. Before final merge, rebuild/rebase/retarget that child onto the newest `main` and verify its final compare contains only the intended milestone.

---

## 2. Branch naming

Use:

```text
agent/mXX-short-description
```

Examples:

```text
agent/m57-reference-gates
agent/m61-campaign-production
agent/m66-mobile-design-system
```

A branch must be created from the latest `main` unless the milestone explicitly documents a temporary dependency.

---

## 3. One milestone = one primary PR

Each milestone should have one primary integration PR into `main`.

The PR description must contain:

1. objective;
2. exact scope;
3. out-of-scope items;
4. data/API/schema migrations;
5. user-facing changes;
6. validation performed;
7. external blockers;
8. Definition of Done checklist;
9. follow-up milestone.

Avoid mixing unrelated cleanup or future milestone work into the PR. If an incidental blocker must be fixed, explain why it is required for the current milestone.

---

## 4. Implementation workflow per milestone

### Step 1 — Read current truth

Before editing:

- read latest `main`;
- read `docs/100-PERCENT-COMPLETION-PLAN.md`;
- read current `docs/12-RELEASE-STATUS.md`;
- inspect open PRs/issues that overlap the milestone;
- identify current schemas/tests/validators for the touched subsystem.

Do not implement from an old conversation summary when the repository has moved forward.

### Step 2 — Freeze milestone contract

Record:

- entry state;
- deliverables;
- non-goals;
- migration compatibility requirements;
- test plan;
- exit/merge gates.

If discoveries materially change scope, update the milestone documentation in the same PR.

### Step 3 — Implement vertically

Prefer complete vertical slices:

```text
data/schema
 -> domain
 -> application service
 -> persistence
 -> API
 -> Unity DTO/store
 -> Unity screen/presentation
 -> localization
 -> tests
```

A backend-only class is not considered a finished gameplay feature when the milestone requires player interaction.

### Step 4 — Validate continuously

Run applicable checks while implementing:

- Java compile/tests;
- Python/static repository validators;
- migration fixtures;
- deterministic/golden tests;
- Unity compile/EditMode/PlayMode tests;
- content/schema validators;
- screenshot/art validators;
- Android build/device checks when relevant.

Never fabricate evidence for a check that cannot run.

### Step 5 — Review final diff against main

Before merge:

- compare milestone head with current `main`;
- confirm no previous milestone is accidentally duplicated/reverted;
- confirm no unrelated file changed;
- review migrations and public contracts carefully;
- review release-state labels so TODO/EXPERIMENTAL values are not falsely marked complete.

### Step 6 — Merge

Preferred default: **squash merge** for a self-contained milestone unless preserving multiple commits materially helps history.

The merge commit/PR title must clearly identify the milestone.

After merge:

- verify `main` contains the intended files/behavior;
- update/close superseded PRs or issues;
- start the next milestone from the new `main`.

---

## 5. Required merge gates by change type

### Server/domain/API changes

Required where applicable:

- compile;
- unit/application/controller tests;
- migration/fixture validation;
- idempotency/transaction tests for rewards/economy;
- authorization/ownership validation;
- API compatibility review.

### Combat changes

Required:

- deterministic same-seed tests;
- event ordering tests;
- timing/status lifecycle tests;
- golden regressions for changed mechanics;
- no hidden reintroduction of round/turn timing;
- Unity replay compatibility validation when event contracts change.

### Unity UI/presentation changes

Required where tooling is available:

- C# compilation;
- EditMode/PlayMode tests;
- navigation smoke;
- null/missing asset fallback in development;
- safe-area/aspect-ratio review;
- screenshot regression for production screens;
- no presentation code mutating authoritative combat/reward state.

### Content changes

Required:

- schema validation;
- referential integrity;
- localization completeness;
- no orphan IDs;
- no unsupported `VERIFIED`/`READY` promotion;
- deterministic simulations when balance/mechanics change.

### Art changes

Required:

- component status validation;
- package descriptor validation;
- real file existence;
- Addressables/runtime resolution;
- regression capture;
- mobile import/compression budget review;
- human review before `READY`.

### Release/Android changes

Required:

- build configuration validator;
- reproducible artifact metadata;
- signing validation for release;
- no secret committed into repository;
- real physical-device evidence for release certification.

---

## 6. CI outages and external blockers

External failures must be classified separately from implementation failures.

Examples:

- GitHub Actions billing prevents runner allocation;
- Unity license credentials are missing;
- signing secrets are not configured;
- physical Android hardware is not available.

Policy:

1. Finish code/static validation that can be completed safely.
2. Document the exact external blocker.
3. Do not claim the blocked check passed.
4. A normal feature milestone may be prepared for merge if its unavailable CI check is non-release and equivalent validation has genuinely been performed elsewhere; this must be documented.
5. A release-certification milestone must not be declared complete while required device/build evidence is unavailable.

---

## 7. Documentation update rule

Any milestone that changes project completion state must update `docs/12-RELEASE-STATUS.md` in the same PR.

Update at least:

- current merged milestone;
- open next milestone;
- relevant completion counters;
- newly cleared blockers;
- newly discovered blockers;
- external dependencies.

Do not leave release status several milestones behind code.

---

## 8. Definition of Done template

Every milestone PR should include this checklist, tailored to its scope:

```text
[ ] Scope implemented
[ ] Data/schema changes validated
[ ] Server tests pass
[ ] Static/content validators pass
[ ] Unity compile/tests pass where applicable
[ ] Migration/compatibility reviewed
[ ] EN/VI localization complete
[ ] No fake READY/VERIFIED/PARITY_PASS state
[ ] Final diff reviewed against latest main
[ ] Release status/docs updated
[ ] External blockers documented
[ ] PR merged into main
```

After the final item is checked, the next milestone branch may begin.

---

## 9. Dependency rules for the current open PRs

Current special case:

```text
main (M53)
 -> M54 PR #72
 -> M55 PR #73
```

Required handling:

1. Validate and merge M54 into `main` first.
2. Refresh M55 against the new `main` so its compare contains only Android/build work.
3. Validate and merge M55 into `main`.
4. From M56 onward, return to the normal one-milestone-from-main workflow.

Legacy open PRs whose behavior was superseded by later merged work should be closed after confirming no unique required changes remain.

---

## 10. Release freeze rule

Once M77 release-candidate hardening begins:

- no unrelated feature expansion;
- only release blocker fixes, evidence, localization, performance, security/config and packaging changes;
- every fix must carry a regression test where practical;
- every release-candidate build must map to an exact `main` SHA;
- the final tag is created only after the same SHA passes release audit and device certification.
