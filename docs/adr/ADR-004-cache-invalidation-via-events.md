# ADR-004: Cache Invalidation via Events

## Context
- Read-heavy endpoints (`GET /products/{id}`, `GET /artists/{id}`) are cached with Spring `@Cacheable` backed by Redis.
- Every write method on `ProductService`/`ArtistService` already carries `@CacheEvict` with the correct key, so the cache stays consistent for today's single-service write path.
- Still, cache invalidation tied only to service-layer writes is fragile if mutations ever come from elsewhere (future write-side split, background job, external producer).

## Decision
- Keep `@CacheEvict` as the primary mechanism. It is enough for the current write paths.
- Add `CacheInvalidationConsumer` as a second channel that evicts the same keys from catalog events (`ProductMetadataUpdated`, `TrackListUpdated`, `ProductPublished`, `ProductTakenDown`, `ArtistUpdated`).
- The consumer is idempotent (ADR-002), so the redundant evict is harmless.

## Why include it at all
- Showcases a meaningful infra consumer alongside the side-effect ones (search index, notification).
- Forward-compatible: if writes ever move out of the service layer, cache consistency already has an event-driven path.

## Alternatives Rejected
- **TTL-only invalidation**
  - Rejected: stale window is unbounded by request timing; not acceptable after a publish/takedown.
- **Synchronous Redis pub/sub from the write path**
  - Rejected: couples the write transaction to Redis availability and bypasses the existing outbox/event pipeline.

## Consequences
- Each write evicts the cache twice (annotation + consumer). Idempotent, but extra work.
- One more consumer group to operate. No new infrastructure.

## Success Criteria
- After a catalog write, subsequent reads reflect the change without waiting for TTL.
- Replaying the same event does not cause incorrect cache state.
