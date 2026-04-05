package com.soundwave.infrastructure.messaging.consumer;

import com.soundwave.domain.entity.ProcessedEvent;
import com.soundwave.infrastructure.messaging.OutboxEnvelope;
import com.soundwave.infrastructure.persistence.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentConsumerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private TestConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TestConsumer(processedEventRepository, objectMapper);
    }

    @Test
    void duplicateEventSkipsProcessing() throws Exception {
        var envelope = validEnvelope();
        when(objectMapper.readValue("payload", OutboxEnvelope.class)).thenReturn(envelope);
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        consumer.handle("payload");

        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
        assertFalse(consumer.processCalled);
    }

    @Test
    void unknownEventTypeFailsFastBeforeProcess() throws Exception {
        var envelope = new OutboxEnvelope(
                "event-1",
                "UnknownType",
                JsonNodeFactory.instance.objectNode().put("productId", "p1")
        );
        when(objectMapper.readValue("payload", OutboxEnvelope.class)).thenReturn(envelope);

        assertThrows(IllegalArgumentException.class, () -> consumer.handle("payload"));
        assertFalse(consumer.processCalled);
    }

    private OutboxEnvelope validEnvelope() {
        return new OutboxEnvelope(
                "event-1",
                "ProductPublished",
                JsonNodeFactory.instance.objectNode()
                        .put("productId", "p1")
                        .put("artistId", "a1")
        );
    }

    private static class TestConsumer extends IdempotentConsumer {
        private boolean processCalled;

        TestConsumer(ProcessedEventRepository processedEventRepository, ObjectMapper objectMapper) {
            super(processedEventRepository, objectMapper);
        }

        @Override
        protected String consumerGroup() {
            return "test-group";
        }

        @Override
        protected void process(OutboxEnvelope envelope) {
            processCalled = true;
        }
    }
}
