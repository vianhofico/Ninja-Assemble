# M77 Support and Incident Runbook

## Severity

- P0: data loss, economy duplication, unauthorized player mutation, widespread login failure or release-signing compromise.
- P1: major gameplay mode unavailable, repeated crashes, severe performance regression or broken purchases/rewards.
- P2: localized feature/UI defect with workaround.
- P3: cosmetic/content issue.

## First response

1. Capture candidate/release commit SHA, server version, Unity client version, player ID, request ID, timestamp and affected endpoint/mode.
2. Preserve server logs and relevant wallet/inventory/action ledgers before manual intervention.
3. For P0 economy/security issues, disable writes or affected endpoints at the edge and preserve evidence before rollback.
4. Never manually edit wallet/inventory balances without an auditable compensation request/key.

## Common diagnostics

- Authentication: verify Bearer token exists, matches player UUID and has not expired.
- Rate limiting: inspect `release-rate:*` Redis keys and backend health; do not disable rate limiting globally to fix one client.
- Rewards: trace requestId/idempotency key through action, wallet and inventory ledgers.
- Competitive: inspect persisted request result and season/attempt rows before replaying an action.
- Android: collect device model, Android version, build fingerprint, artifact ref and M76 probe output.

## Recovery

Use `M77_DATABASE_BACKUP_ROLLBACK.md` for database incidents. For application-only regressions, roll back to the previous signed artifact and verify health/login/state reads before re-opening traffic.

## Support publication blocker

The final store support URL/contact channel is external operational evidence. `m77-store-metadata.json` deliberately keeps it `PENDING_PUBLIC_URL` until a real public endpoint exists.
