package com.tourpackage.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.entity.EmailVerificationToken;
import com.tourpackage.api.repository.AdminRepository;
import com.tourpackage.api.repository.EmailVerificationTokenRepository;
import com.tourpackage.api.security.TokenGenerator;

/**
 * Side effects of a login attempt that must persist independently of how
 * {@link AuthService#login} itself resolves. Both methods here run in
 * {@link Propagation#REQUIRES_NEW}: {@code login()} always throws after
 * calling into this class (invalid credentials, or "please verify your
 * email"), and Spring's default rollback-on-RuntimeException would silently
 * undo these writes if they shared the caller's transaction — exactly the
 * bug this class exists to avoid (see refresh-token reuse detection in
 * {@link TokenRevocationService} for the same pattern).
 */
@Service
public class LoginSecurityService {

    private final AdminRepository adminRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TokenGenerator tokenGenerator;
    private final MailService mailService;

    private final int maxFailedAttempts;
    private final Duration lockDuration;
    private final Duration emailVerificationTtl;

    public LoginSecurityService(
            AdminRepository adminRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            TokenGenerator tokenGenerator,
            MailService mailService,
            @Value("${app.security.max-failed-login-attempts}") int maxFailedAttempts,
            @Value("${app.security.account-lock-duration-minutes}") long lockDurationMinutes,
            @Value("${app.email-verification.token-expiration-hours}") long emailVerificationTtlHours) {
        this.adminRepository = adminRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.mailService = mailService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
        this.emailVerificationTtl = Duration.ofHours(emailVerificationTtlHours);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registerFailedAttempt(String email) {
        adminRepository.findByEmailIgnoreCase(email).ifPresent(admin -> {
            int attempts = admin.getFailedLoginAttempts() + 1;
            if (attempts >= maxFailedAttempts) {
                admin.setLockedUntil(Instant.now().plus(lockDuration));
                admin.setFailedLoginAttempts((short) 0);
            } else {
                admin.setFailedLoginAttempts((short) attempts);
            }
            adminRepository.save(admin);
        });
    }

    /**
     * Skips sending if a still-valid token is already pending, so repeated
     * failed login attempts against an unverified account don't flood the
     * inbox.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureVerificationEmailSent(UUID adminId, String email, String fullName) {
        boolean hasPendingToken = emailVerificationTokenRepository
                .findFirstByAdminIdAndVerifiedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(adminId, Instant.now())
                .isPresent();
        if (hasPendingToken) {
            return;
        }

        String rawToken = tokenGenerator.generateRawToken();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .adminId(adminId)
                .tokenHash(tokenGenerator.hash(rawToken))
                .expiresAt(Instant.now().plus(emailVerificationTtl))
                .createdAt(Instant.now())
                .build();
        emailVerificationTokenRepository.save(token);
        mailService.sendEmailVerificationEmail(email, fullName, rawToken);
    }

}
