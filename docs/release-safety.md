# Release Safety

## Migration Safety
- Forward-only Flyway migrations.
- Prefer expand-then-contract for schema changes.
- Validate migration on staging before production.
- Assume DB rollback is not automatic once migration is applied.
- If rollback script is needed, prepare and review it before production rollout.

## Compatibility
- API: avoid in-place breaking request/response changes.
- Events: additive payload policy; breaking changes get a new event type.

## Rollback Plan
1. Stop rollout.
2. Roll back application artifact.
3. Keep DB on latest migration unless a reviewed rollback script exists.
4. If schema fix is needed, prefer a forward corrective migration.
5. Review outbox failures/DLT before retry.