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
- Kafka consumer lag (exporter required in production)

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
6. **Kafka Consumer Lag**
   - Query depends on lag exporter metric name (for example `kafka_consumergroup_lag`)

## Alert Baseline
- Pending high: `outbox_events_pending > 100 for 5m`
- Failed rows: `outbox_events_failed > 0 for 2m`
- DLT spike: `increase(outbox_dlt_events_total[10m]) > 0`

## Scope Note
Current assignment includes datasource provisioning and alert rules. Dashboard JSON provisioning can be added as next step.
