# M77 Final Release Runbook

This is the final mobile release procedure. It deliberately separates **implementation-complete main** from a **certified release candidate**.

## 1. Freeze candidate SHA

Select one commit on `main`. Do not move the candidate while collecting M74/M75/M76/operator evidence; all strict evidence is bound to the exact candidate SHA.

## 2. Prepare production configuration

Activate the `prod` Spring profile and provide `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PASSWORD` and a high-entropy `GAME_SESSION_SECRET` through the deployment secret store. Do not commit those values. Confirm health probes, Redis-backed rate limiting, security headers and signed player-session authorization.

## 3. Database restore rehearsal

Follow `M77_DATABASE_BACKUP_ROLLBACK.md`: create a pre-release backup, restore it to a disposable target, run Flyway with the exact candidate server, execute health/restart checks, and retain the backup checksum + restore/migration logs as `DATABASE_RESTORE` operator evidence.

## 4. Evidence gates

Collect only real evidence for:

- M74: ten VERIFIED reference profiles, full-roster review and feature PARITY_PASS.
- M75: exact-SHA 20-step E2E + ten reliability PASS report with logs.
- M76: at least two physical Android models across at least two classes passing benchmark/smoke thresholds.
- Art: 427/427 repository-backed READY packages.
- Operator: `DATABASE_RESTORE`, `SIGNED_AAB`, `STORE_REVIEW`, `SUPPORT_ENDPOINT`, `RIGHTS_CLEARANCE` PASS rows on the exact candidate SHA.
- Provenance: every production dependency/asset/IP scope has a documented license/right reference and `READY` status.

## 5. Android release build

Configure Unity credentials plus Android keystore secrets. Build the Release AAB from the exact candidate SHA, retain `build-metadata.json`, artifact checksum/signing evidence and record it as `SIGNED_AAB` operator evidence. Do not reuse a build from another commit.

## 6. Store/operator readiness

Replace all `PENDING_*` metadata with real public privacy/support URLs and completed content-rating/data-safety review. Record store review/support endpoint evidence. Do not publish Naruto-derived or other third-party IP until `RIGHTS_CLEARANCE` is documented, or replace that content with original/licensed content.

## 7. Run final gate

Run:

`python scripts/validate-m77-release-hardening.py --enforce`

Then dispatch `production-release-gate` on the exact candidate SHA. Both must pass. A failing evidence gate is a release blocker, not a warning.

## 8. Tag/release

Only after strict M77 + production workflow are green may an RC tag be created. Release notes must reference the same SHA, signed AAB and evidence bundle.

## 9. Post-deploy

Verify health, guest login/session authorization, player state, one safe Campaign read/battle in the release environment, rate limiting, and store delivery. For data/security/economy regressions follow the support + rollback runbooks.
