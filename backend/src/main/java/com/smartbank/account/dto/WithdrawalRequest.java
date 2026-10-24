package com.smartbank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WithdrawalRequest(

        @NotNull(message = "Withdrawal amount is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "Withdrawal amount must be greater than zero"
        )
        BigDecimal amount

) {
}
