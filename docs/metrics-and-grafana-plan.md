# Metrics and Grafana Plan

## Core Metrics to Keep
- `outbox.events.pending` (`outbox_events_pending`) — gauge, current backlog
- `outbox.events.failed` (`outbox_events_failed`) — gauge, permanently failed rows
- `outbox.events.published` (`outbox_events_published_total`) — counter, throughput
- `outbox.events.publish.failures` (`outbox_events_publish_failures_total`) — counter, transient + permanent
- `outbox.publish.duration` (`outbox_publish_duration_seconds_*`) — timer, per-event Kafka publish latency
- `outbox.publish.batch.size` (`outbox_publish_batch_size_*`) — distribution, batch size per flush
- `outbox.dlt.events` (`outbox_dlt_events_total`)
- `http.server.requests` (`http_server_requests_seconds_*`)
- Kafka consumer lag — **not emitted by this app or by Kafka itself**. Requires deploying a separate exporter (e.g. `kafka_exporter` → `kafka_consumergroup_lag`). Not provisioned in this scope.

## Recommended Dashboard Panels
1. **Outbox Pending (gauge + 15m trend)**
   - Query: `outbox_events_pending`
2. **Outbox Failed (gauge)**
   - Query: `outbox_events_failed`
3. **DLT Rate (events/min)**
   - Query: `increase(outbox_dlt_events_total[5m])`
4. **API Error Rate (5xx)**
   - Query: `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`
5. **P95 API Latency**
   - Query: `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket[5m])))`
6. **Kafka Consumer Lag** — deferred. Requires a Kafka lag exporter, which is not deployed. See Scope Note.

## Alert Baseline
- Pending high: `outbox_events_pending > 100 for 5m`
- Failed rows: `outbox_events_failed > 0 for 2m`
- DLT spike: `increase(outbox_dlt_events_total[10m]) > 0`

## Provisioned Dashboard
- **Soundwave — Outbox Health** (`soundwave-outbox-health`), folder: *Soundwave*
- Source: [`monitoring/grafana/dashboards/outbox-health.json`](../monitoring/grafana/dashboards/outbox-health.json)
- Loaded declaratively via the file provider in [`provisioning/dashboards/dashboards.yml`](../monitoring/grafana/provisioning/dashboards/dashboards.yml); no UI import needed.
- Panels: pending / failed / published / DLT stats, backlog trend, publish throughput vs failures, publish latency (mean + max), batch size, DLT rate.
- Open: http://localhost:3000 → Dashboards → Soundwave → Outbox Health.

## Scope Note
Datasource + dashboards + alert rules are all provisioned from disk. API latency and consumer-lag dashboards are the natural next additions.
