package com.soundwave.api.contract;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record MoneyDto(
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Pattern(regexp = "(?i)^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code") String currency
) {
}
