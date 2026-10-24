package com.smartbank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull(message = "Destination account ID is required")
        @Positive(message = "Destination account ID must be positive")
        Long destinationAccountId,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Transfer amount must be greater than zero"
        )
        BigDecimal amount

) {
}
