package com.soundwave.infrastructure.messaging.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterLogConsumer {

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
        log.atError()
                .addKeyValue("topic", topic)
                .addKeyValue("partition", partition)
                .addKeyValue("offset", offset)
                .addKeyValue("payload", payload)
                .log("Dead-letter event received");
    }
}
