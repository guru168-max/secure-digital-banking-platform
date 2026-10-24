package com.smartbank.account.repository;

import com.smartbank.account.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository
        extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    List<BankAccount> findAllByUser_Id(Long userId);

    Optional<BankAccount> findByIdAndUser_Id(
            Long accountId,
            Long userId
    );

    boolean existsByAccountNumber(String accountNumber);
}
