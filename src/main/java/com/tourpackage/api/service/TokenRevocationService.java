package com.tourpackage.api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.repository.RefreshTokenRepository;

/**
 * Isolated in its own bean so the revoke-all-sessions call can run in
 * {@link Propagation#REQUIRES_NEW}. Reuse detection in
 * {@link AuthService#refresh} revokes every active refresh token for the
 * admin and then throws — if that revocation shared the caller's
 * transaction, Spring's default rollback-on-RuntimeException would undo it,
 * silently defeating the whole point of reuse detection. A separate
 * transaction commits immediately regardless of what the caller does next.
 */
@Service
public class TokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    public TokenRevocationService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllActiveForAdmin(UUID adminId) {
        refreshTokenRepository.revokeAllActiveForAdmin(adminId, Instant.now());
    }

}
