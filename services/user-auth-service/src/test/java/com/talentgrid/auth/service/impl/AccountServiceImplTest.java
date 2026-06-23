package com.talentgrid.auth.service.impl;

import com.talentgrid.auth.dto.request.CreateAccountRequest;
import com.talentgrid.auth.entity.Account;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.exception.ResourceNotFoundException;
import com.talentgrid.auth.mapper.AccountMapper;
import com.talentgrid.auth.repository.AccountRepository;
import com.talentgrid.auth.repository.UserRepository;
import com.talentgrid.auth.service.interfaces.ResourceAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for account CRUD authorization boundaries in {@link AccountServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private ResourceAuthorizationService resourceAuthorizationService;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void createAccountRequiresAdmin() {
        doThrow(new AccessDeniedException("denied"))
                .when(resourceAuthorizationService).requireAdmin();

        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Acme");
        request.setAccountManagerId(1L);

        assertThrows(AccessDeniedException.class, () -> accountService.createAccount(request));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAccountByIdForLookupThrowsWhenMissing() {
        when(accountRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getAccountByIdForLookup(42L));
    }

    @Test
    void updateAccountChecksOwnershipAfterLoad() {
        Account account = Account.builder().id(1L).name("Old").accountManagerId(2L).build();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        doThrow(new AccessDeniedException("denied"))
                .when(resourceAuthorizationService).requireCanManageAccount(1L);

        assertThrows(AccessDeniedException.class, () ->
                accountService.updateAccount(1L, updateRequest("New", 2L)));
    }

    @Test
    void createAccountValidatesAccountManagerExists() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Acme");
        request.setAccountManagerId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(request));
    }

    private static com.talentgrid.auth.dto.request.UpdateAccountRequest updateRequest(
            String name, Long accountManagerId) {
        com.talentgrid.auth.dto.request.UpdateAccountRequest request =
                new com.talentgrid.auth.dto.request.UpdateAccountRequest();
        request.setName(name);
        request.setAccountManagerId(accountManagerId);
        return request;
    }
}
