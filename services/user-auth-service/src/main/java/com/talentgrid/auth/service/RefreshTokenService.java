package com.talentgrid.auth.service;

import com.talentgrid.auth.entity.RefreshToken;
import com.talentgrid.auth.entity.User;
import com.talentgrid.auth.jwt.JwtService;
import com.talentgrid.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsible for managing the lifecycle of refresh tokens.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Create new refresh tokens upon login</li>
 *   <li>Validate existing refresh tokens (existence, expiration)</li>
 *   <li>Rotate refresh tokens during the refresh flow</li>
 *   <li>Revoke tokens upon logout</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtService jwtService;

    /**
     * Creates a new refresh token for the given user, replacing any existing one.
     *
     * @param user the user requesting a token
     * @return the saved RefreshToken entity
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {

        refreshTokenRepository.findByUser(user)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    refreshTokenRepository.flush(); // 👈 ADD THIS
                });

        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwtService.generateRefreshToken())
                .user(user)
                .expiryDate(
                        LocalDateTime.now()
                                .plusSeconds(
                                        jwtService.getRefreshExpiration() / 1000
                                )
                )
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates a raw refresh token string.
     *
     * @param token the refresh token string
     * @return the valid RefreshToken entity
     * @throws RuntimeException if the token is invalid or expired
     */
    @Transactional
    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() ->
                        new RuntimeException("Invalid refresh token")
                );

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {

            throw new RuntimeException("Refresh token expired");
        }

        return refreshToken;
    }


    /**
     * Deletes any existing refresh token for the user.
     *
     * @param user the user logging out
     */
    @Transactional
    public void revokeRefreshToken(User user) {

        refreshTokenRepository.findByUser(user)
                .ifPresent(refreshTokenRepository::delete);
    }

    /**
     * Deletes a specific refresh token by its string value.
     *
     * @param token the token string
     */
    @Transactional
    public void deleteByToken(String token) {

        refreshTokenRepository.deleteByToken(token);
    }

    /**
     * Rotates an existing refresh token by replacing its string value
     * and resetting its expiration date.
     *
     * @param existingToken the old token entity
     * @return the updated RefreshToken entity
     */
    @Transactional
    public RefreshToken rotateRefreshToken(
            RefreshToken existingToken
    ) {

        existingToken.setToken(
                jwtService.generateRefreshToken()
        );
        
        // Reset the expiry date to grant full lifetime to the new rotated token
        existingToken.setExpiryDate(
                LocalDateTime.now()
                        .plusSeconds(
                                jwtService.getRefreshExpiration() / 1000
                        )
        );

        return refreshTokenRepository.save(existingToken);
    }

}
