package com.talentgrid.auth.service.interfaces;

import com.talentgrid.auth.dto.request.CreateAccountRequest;
import com.talentgrid.auth.dto.request.UpdateAccountRequest;
import com.talentgrid.auth.dto.response.AccountResponse;

import java.util.List;

/**
 * Service contract for account CRUD operations.
 */
public interface AccountService {

    /**
     * Creates a new account. Caller must be an administrator.
     *
     * @param request validated create payload
     * @return created account
     */
    AccountResponse createAccount(CreateAccountRequest request);

    /**
     * Returns an account by id after authorization checks.
     *
     * @param id account id
     * @return account response
     */
    AccountResponse getAccountById(Long id);

    /**
     * Returns an account by id for authenticated internal lookups (no ownership check).
     *
     * @param id account id
     * @return account response
     */
    AccountResponse getAccountByIdForLookup(Long id);

    /**
     * Lists accounts visible to the current user.
     *
     * @return authorized account list
     */
    List<AccountResponse> getAllAccounts();

    /**
     * Updates an account when the caller is authorized.
     *
     * @param id      account id
     * @param request validated update payload
     * @return updated account
     */
    AccountResponse updateAccount(Long id, UpdateAccountRequest request);

    /**
     * Deletes an account when the caller is authorized.
     *
     * @param id account id
     */
    void deleteAccount(Long id);
}
