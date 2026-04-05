package com.soundwave.infrastructure.messaging.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterLogConsumer {

    private final Counter dltCounter;

    public DeadLetterLogConsumer(MeterRegistry meterRegistry) {
        this.dltCounter = Counter.builder("outbox.dlt.events")
                .description("Number of events landed in dead-letter topic")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = {"catalog.product.events.dlt", "catalog.artist.events.dlt"},
            groupId = "dlt-log-group"
    )
    public void listen(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        dltCounter.increment();
        log.atError()
                .addKeyValue("topic", topic)
                .addKeyValue("partition", partition)
                .addKeyValue("offset", offset)
                .addKeyValue("payloadSize", payload == null ? 0 : payload.length())
                .log("Dead-letter event received");
    }
}
