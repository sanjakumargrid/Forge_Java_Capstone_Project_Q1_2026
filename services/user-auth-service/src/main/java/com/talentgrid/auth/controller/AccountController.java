package com.talentgrid.auth.controller;

import com.talentgrid.auth.dto.request.CreateAccountRequest;
import com.talentgrid.auth.dto.request.UpdateAccountRequest;
import com.talentgrid.auth.dto.response.AccountResponse;
import com.talentgrid.auth.service.interfaces.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for account CRUD operations.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>Create: administrators only</li>
 *   <li>Read list / update / delete: administrator or assigned account manager</li>
 *   <li>Read by id: any authenticated caller (supports inter-service lookups)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Creates a new account.
     *
     * POST /api/v1/accounts
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return ResponseEntity.ok(accountService.createAccount(request));
    }

    /**
     * Returns an account by id.
     *
     * <p>Available to any authenticated principal so downstream services can denormalize account names.
     *
     * GET /api/v1/accounts/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccountByIdForLookup(id));
    }

    /**
     * Lists accounts visible to the current user.
     *
     * GET /api/v1/accounts
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    /**
     * Updates an account.
     *
     * PUT /api/v1/accounts/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageAccount(#id)")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    /**
     * Deletes an account.
     *
     * DELETE /api/v1/accounts/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@resourceAuthorizationService.canManageAccount(#id)")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
