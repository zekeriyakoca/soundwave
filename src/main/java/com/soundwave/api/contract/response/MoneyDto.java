package com.soundwave.api.contract.response;

import java.math.BigDecimal;

public record MoneyDto(BigDecimal amount, String currency) {
}
