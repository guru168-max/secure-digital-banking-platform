package com.smartbank.account.dto;

import com.smartbank.account.entity.BankAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankAccountResponse(
        Long id,
        String accountNumber,
        BigDecimal balance,
        String status,
        LocalDateTime createdAt
) {
    public static BankAccountResponse from(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}
