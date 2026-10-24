package com.smartbank.account.controller;


import com.smartbank.account.dto.TransferRequest;
import com.smartbank.transaction.service.TransactionService;
import com.smartbank.account.dto.BankAccountResponse;
import com.smartbank.account.dto.DepositRequest;
import com.smartbank.account.dto.WithdrawalRequest;
import com.smartbank.account.entity.BankAccount;
import com.smartbank.account.service.BankAccountService;
import com.smartbank.transaction.dto.TransactionResponse;
import com.smartbank.transaction.service.TransactionService;
import com.smartbank.user.entity.User;
import com.smartbank.user.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public BankAccountController(
            BankAccountService bankAccountService,
            TransactionService transactionService,
            UserRepository userRepository) {
        this.bankAccountService = bankAccountService;
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BankAccountResponse createAccount(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        BankAccount account =
                bankAccountService.createAccount(user.getId());

        return BankAccountResponse.from(account);
    }

    @GetMapping
    public List<BankAccountResponse> getMyAccounts(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return bankAccountService.getMyAccounts(user.getId())
                .stream()
                .map(BankAccountResponse::from)
                .toList();
    }

    @GetMapping("/{accountId}")
    public BankAccountResponse getMyAccount(
            @PathVariable Long accountId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        BankAccount account = bankAccountService.getMyAccount(
                accountId,
                user.getId()
        );

        return BankAccountResponse.from(account);
    }

    @GetMapping("/{accountId}/transactions")
    public List<TransactionResponse> getMyTransactions(
            @PathVariable Long accountId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        return transactionService.getMyTransactions(
                accountId,
                user.getId()
        );
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }

    @PostMapping("/{accountId}/deposit")
    public BankAccountResponse deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        BankAccount account = bankAccountService.deposit(
                accountId,
                user.getId(),
                request.amount()
        );

        return BankAccountResponse.from(account);
    }

    @PostMapping("/{accountId}/withdraw")
    public BankAccountResponse withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody WithdrawalRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        BankAccount account = bankAccountService.withdraw(
                accountId,
                user.getId(),
                request.amount()
        );

        return BankAccountResponse.from(account);
    }



    @PostMapping("/{accountId}/transfer")
    public BankAccountResponse transfer(
            @PathVariable Long accountId,
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        bankAccountService.transfer(
                accountId,
                user.getId(),
                request.destinationAccountId(),
                request.amount()
        );

        // Return the updated source account.
        BankAccount sourceAccount = bankAccountService.getMyAccount(
                accountId,
                user.getId()
        );

        return BankAccountResponse.from(sourceAccount);
    }

}
