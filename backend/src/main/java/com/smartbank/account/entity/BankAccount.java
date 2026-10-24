package com.smartbank.account.entity;

import com.smartbank.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "bank_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bank_accounts_account_number",
                        columnNames = "account_number"
                )
        }
)
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "account_number",
            nullable = false,
            length = 20
    )
    private String accountNumber;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected BankAccount() {
        // Required by JPA
    }

    public BankAccount(String accountNumber, User user) {
        this.accountNumber = accountNumber;
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public User getUser() {
        return user;
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Deposit amount must be greater than zero"
            );
        }

        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException(
                    "Cannot deposit into an inactive account"
            );
        }

        this.balance = this.balance.add(amount);
    }


    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Withdrawal amount must be greater than zero"
            );
        }

        if (!"ACTIVE".equals(status)) {
            throw new IllegalStateException(
                    "Cannot withdraw from an inactive account"
            );
        }

        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient account balance"
            );
        }

        this.balance = this.balance.subtract(amount);
    }

}
