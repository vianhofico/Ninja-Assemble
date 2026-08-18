# M77 Database Backup and Rollback Runbook

This runbook is an operator procedure. A production release is not certified merely because this document exists; the release candidate must retain real backup/restore evidence.

## Pre-release backup

Required environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`. Before applying a release migration, record the candidate commit SHA and current Flyway schema history, then create a PostgreSQL custom-format backup with `pg_dump --format=custom --no-owner --no-privileges` to an access-controlled artifact location. Record the backup checksum, database timestamp and operator identity in the release ticket/evidence bundle.

## Migration gate

1. Restore the backup into a disposable database.
2. Start the exact candidate server against the disposable restore.
3. Allow Flyway to migrate through the candidate migration set.
4. Run server health checks and the M75 migration/restart-persistence cases.
5. Do not deploy if restore, migration, health or data verification fails.

## Application rollback

If the release contains no irreversible data migration, redeploy the previous signed server/client artifact and verify health, player login, wallet/inventory reads and one non-destructive state query.

## Database rollback

Flyway migrations in this repository are forward migrations; do not improvise destructive down-SQL in production. If a new migration corrupts data or is not backward compatible:

1. Stop writes.
2. Preserve the failed database for diagnosis.
3. Restore the verified pre-release backup to a clean database target.
4. Point the previous application release at the restored target.
5. Verify schema history, row counts for critical player/economy tables and a sample of player state.
6. Re-open writes only after verification.

## Required evidence

The RC evidence bundle must include backup artifact reference/checksum, restore-test log, Flyway migration log, previous and candidate commit SHAs, operator timestamp, and rollback outcome. M77 certification remains PENDING until that evidence is attached outside the source-only framework.
