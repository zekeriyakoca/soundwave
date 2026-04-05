package com.soundwave.infrastructure.messaging.payload;

import java.math.BigDecimal;

public record MoneyPayload(
        BigDecimal amount,
        String currency
) {
}
