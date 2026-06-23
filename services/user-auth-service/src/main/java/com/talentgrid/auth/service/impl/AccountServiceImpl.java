package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.CreateAccountRequest;
import com.talentgrid.auth.dto.request.UpdateAccountRequest;
import com.talentgrid.auth.dto.response.AccountResponse;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.exception.ResourceNotFoundException;
import com.talentgrid.auth.mapper.AccountMapper;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.AccountService;
import com.talentgrid.auth.service.interfaces.ResourceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AccountService} with role- and ownership-based authorization.
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final ResourceAuthorizationService resourceAuthorizationService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        resourceAuthorizationService.requireAdmin();
        validateAccountManager(request.getAccountManagerId());

        if (accountRepository.existsByName(request.getName())) {
            throw new RuntimeException("Account name already exists: " + request.getName());
        }

        Account account = Account.builder()
                .name(request.getName())
                .accountManagerId(request.getAccountManagerId())
                .build();

        return accountMapper.toResponse(accountRepository.save(account));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = findAccountOrThrow(id);
        resourceAuthorizationService.requireCanManageAccount(account.getId());
        return accountMapper.toResponse(account);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByIdForLookup(Long id) {
        return accountMapper.toResponse(findAccountOrThrow(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        if (resourceAuthorizationService.isAdmin()) {
            return accountRepository.findAll().stream()
                    .map(accountMapper::toResponse)
                    .collect(Collectors.toList());
        }

        Long currentUserId = resourceAuthorizationService.getCurrentUserId();
        List<Account> managedAccounts = accountRepository.findByAccountManagerId(currentUserId);
        if (managedAccounts.isEmpty()) {
            throw new AccessDeniedException("You do not have permission to view accounts");
        }

        return managedAccounts.stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AccountResponse updateAccount(Long id, UpdateAccountRequest request) {
        Account account = findAccountOrThrow(id);
        resourceAuthorizationService.requireCanManageAccount(account.getId());
        validateAccountManager(request.getAccountManagerId());

        if (accountRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new RuntimeException("Account name already exists: " + request.getName());
        }

        account.setName(request.getName());
        account.setAccountManagerId(request.getAccountManagerId());

        return accountMapper.toResponse(accountRepository.save(account));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = findAccountOrThrow(id);
        resourceAuthorizationService.requireCanManageAccount(account.getId());
        accountRepository.delete(account);
    }

    private Account findAccountOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    private void validateAccountManager(Long accountManagerId) {
        User accountManager = userRepository.findById(accountManagerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account manager user not found with id: " + accountManagerId));

        if (!Boolean.TRUE.equals(accountManager.getEnabled())) {
            throw new RuntimeException("Account manager user is disabled: " + accountManagerId);
        }
    }
}
