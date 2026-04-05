package com.soundwave.infrastructure.messaging;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CatalogEventContractTest {

    @Nested
    class ValidateEnvelope {

        @Test
        void accepts_whenSupportedEventType() {
            var payload = JsonNodeFactory.instance.objectNode().put("productId", "p1");
            var envelope = new OutboxEnvelope(
                    "event-1",
                    CatalogEventSchema.PRODUCT_PUBLISHED,
                    payload
            );

            assertDoesNotThrow(() -> CatalogEventSchema.validateEnvelope(envelope));
        }

        @Test
        void rejects_whenMissingPayload() {
            var envelope = new OutboxEnvelope(
                    "event-1",
                    CatalogEventSchema.PRODUCT_PUBLISHED,
                    null
            );

            assertThrows(IllegalArgumentException.class, () ->
                    CatalogEventSchema.validateEnvelope(envelope)
            );
        }

        @Test
        void rejects_whenBlankEventId() {
            var payload = JsonNodeFactory.instance.objectNode().put("productId", "p1");
            var envelope = new OutboxEnvelope(
                    "  ",
                    CatalogEventSchema.PRODUCT_PUBLISHED,
                    payload
            );

            assertThrows(IllegalArgumentException.class, () ->
                    CatalogEventSchema.validateEnvelope(envelope)
            );
        }
    }

    @Nested
    class ValidateEventType {

        @Test
        void rejects_whenUnknownType() {
            assertThrows(IllegalArgumentException.class, () ->
                    CatalogEventSchema.validateEventType("UnknownEventType")
            );
        }
    }
}
