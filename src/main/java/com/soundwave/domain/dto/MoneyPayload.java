package com.soundwave.domain.dto;

import java.math.BigDecimal;

public record MoneyPayload(
        BigDecimal amount,
        String currency
) {
}
