# ADR-004: Cache Invalidation via Events

## Context
- Read-heavy endpoints (`GET /products/{id}`, `GET /artists/{id}`) are cached with Spring `@Cacheable` backed by Redis.
- Local `@CacheEvict` on the writing service method works for the writing instance only. With multiple app instances, other nodes still serve stale data from their own cache view of Redis after a write that bypassed them.
- Cache must stay consistent with the catalog without coupling reads to the database on every request.

## Decision
- Treat the cache as a downstream consumer of catalog events, not as a side concern of the write path.
- `CacheInvalidationConsumer` listens to catalog topics and evicts the relevant cache entry on:
  `ProductMetadataUpdated`, `TrackListUpdated`, `ProductPublished`, `ProductTakenDown`, `ArtistUpdated`.
- Local `@CacheEvict` is kept for the writing instance as a fast path; the event-driven evict is the source of truth across instances.
- The consumer is idempotent (ADR-002), so duplicate evicts are harmless.

## Alternatives Rejected
- **Local `@CacheEvict` only**
  - Rejected: does not invalidate other instances' Redis access patterns or cached values written by other nodes.
- **TTL-only invalidation**
  - Rejected: stale window is unbounded by request timing; not acceptable for catalog reads after a publish/takedown.
- **Synchronous Redis pub/sub from the write path**
  - Rejected: couples the write transaction to Redis availability and bypasses the existing outbox/event pipeline.

## Consequences
- Cache consistency reuses the same delivery and idempotency guarantees as the rest of the system.
- A short eventual-consistency window exists between commit and consumer evict; acceptable for a catalog read model.
- Adds one more consumer group to operate, but no new infrastructure.

## Success Criteria
- After a published product is updated on instance A, a read on instance B reflects the change without waiting for TTL.
- Replaying the same event does not cause incorrect cache state.
