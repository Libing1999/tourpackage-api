package com.tourpackage.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.ForgotPasswordRequest;
import com.tourpackage.api.dto.request.LoginRequest;
import com.tourpackage.api.dto.request.RefreshTokenRequest;
import com.tourpackage.api.dto.request.ResetPasswordRequest;
import com.tourpackage.api.dto.request.UpdateProfileRequest;
import com.tourpackage.api.dto.response.AdminProfileResponse;
import com.tourpackage.api.dto.response.AuthResponse;
import com.tourpackage.api.entity.Admin;
import com.tourpackage.api.entity.EmailVerificationToken;
import com.tourpackage.api.entity.PasswordResetToken;
import com.tourpackage.api.entity.RefreshToken;
import com.tourpackage.api.exception.AccountDisabledException;
import com.tourpackage.api.exception.AccountLockedException;
import com.tourpackage.api.exception.EmailNotVerifiedException;
import com.tourpackage.api.exception.InvalidCredentialsException;
import com.tourpackage.api.exception.InvalidOrExpiredTokenException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.mapper.AdminMapper;
import com.tourpackage.api.repository.AdminRepository;
import com.tourpackage.api.repository.EmailVerificationTokenRepository;
import com.tourpackage.api.repository.PasswordResetTokenRepository;
import com.tourpackage.api.repository.RefreshTokenRepository;
import com.tourpackage.api.security.AdminUserDetails;
import com.tourpackage.api.security.JwtService;
import com.tourpackage.api.security.TokenGenerator;

@Service
@Transactional
public class AuthService {

    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenGenerator tokenGenerator;
    private final AdminMapper adminMapper;
    private final MailService mailService;
    private final TokenRevocationService tokenRevocationService;
    private final LoginSecurityService loginSecurityService;

    private final Duration refreshTokenTtl;
    private final Duration passwordResetTtl;

    public AuthService(
            AdminRepository adminRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenGenerator tokenGenerator,
            AdminMapper adminMapper,
            MailService mailService,
            TokenRevocationService tokenRevocationService,
            LoginSecurityService loginSecurityService,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenTtlMs,
            @Value("${app.password-reset.token-expiration-minutes}") long passwordResetTtlMinutes) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenGenerator = tokenGenerator;
        this.adminMapper = adminMapper;
        this.mailService = mailService;
        this.tokenRevocationService = tokenRevocationService;
        this.loginSecurityService = loginSecurityService;
        this.refreshTokenTtl = Duration.ofMillis(refreshTokenTtlMs);
        this.passwordResetTtl = Duration.ofMinutes(passwordResetTtlMinutes);
    }

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String normalizedEmail = normalize(request.email());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        } catch (BadCredentialsException ex) {
            // Must commit in its own transaction — this method throws right
            // after, which would otherwise roll the write back. See
            // LoginSecurityService's class javadoc.
            loginSecurityService.registerFailedAttempt(normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (LockedException ex) {
            throw new AccountLockedException("Account is temporarily locked due to repeated failed login attempts");
        } catch (DisabledException ex) {
            throw new AccountDisabledException("Account is disabled. Contact a super admin.");
        }

        Admin admin = ((AdminUserDetails) authentication.getPrincipal()).getAdmin();

        if (!admin.isEmailVerified()) {
            loginSecurityService.ensureVerificationEmailSent(admin.getId(), admin.getEmail(), admin.getFullName());
            throw new EmailNotVerifiedException("Please verify your email before logging in. "
                    + "A new verification link has been sent if one wasn't already pending.");
        }

        admin.setFailedLoginAttempts((short) 0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(Instant.now());
        adminRepository.save(admin);

        IssuedTokens issued = issueTokens(admin, ipAddress, userAgent);
        return toAuthResponse(issued, admin);
    }

    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String hash = tokenGenerator.hash(request.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid refresh token"));

        if (existing.getRevokedAt() != null) {
            // A revoked token being presented again means it was stolen/replayed
            // (rotation makes the old token single-use) — kill every active
            // session for this admin, not just this one.
            tokenRevocationService.revokeAllActiveForAdmin(existing.getAdminId());
            throw new InvalidOrExpiredTokenException("Refresh token has already been used; all sessions revoked");
        }
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidOrExpiredTokenException("Refresh token has expired");
        }

        Admin admin = adminRepository.findById(existing.getAdminId())
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid refresh token"));

        if (!admin.isActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        IssuedTokens issued = issueTokens(admin, ipAddress, userAgent);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedById(issued.refreshTokenEntity().getId());
        refreshTokenRepository.save(existing);

        return toAuthResponse(issued, admin);
    }

    public void forgotPassword(ForgotPasswordRequest request, String ipAddress) {
        String normalizedEmail = normalize(request.email());

        adminRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(Admin::isActive)
                .ifPresent(admin -> {
                    String rawToken = tokenGenerator.generateRawToken();
                    PasswordResetToken token = PasswordResetToken.builder()
                            .adminId(admin.getId())
                            .tokenHash(tokenGenerator.hash(rawToken))
                            .expiresAt(Instant.now().plus(passwordResetTtl))
                            .createdByIp(ipAddress)
                            .createdAt(Instant.now())
                            .build();
                    passwordResetTokenRepository.save(token);
                    mailService.sendPasswordResetEmail(admin.getEmail(), admin.getFullName(), rawToken);
                });
        // Response is identical whether or not the email exists — the caller
        // (controller) always returns the same generic message.
    }

    public void resetPassword(ResetPasswordRequest request) {
        String hash = tokenGenerator.hash(request.token());
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired reset token"));

        Admin admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired reset token"));

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        admin.setFailedLoginAttempts((short) 0);
        admin.setLockedUntil(null);
        adminRepository.save(admin);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        // A password reset invalidates every existing session, not just the
        // device that requested it.
        tokenRevocationService.revokeAllActiveForAdmin(admin.getId());
    }

    public void verifyEmail(String rawToken) {
        String hash = tokenGenerator.hash(rawToken);
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash)
                .filter(EmailVerificationToken::isUsable)
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired verification token"));

        Admin admin = adminRepository.findById(token.getAdminId())
                .orElseThrow(() -> new InvalidOrExpiredTokenException("Invalid or expired verification token"));

        admin.setEmailVerifiedAt(Instant.now());
        adminRepository.save(admin);

        token.setVerifiedAt(Instant.now());
        emailVerificationTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public AdminProfileResponse getProfile(UUID adminId) {
        return adminMapper.toProfileResponse(findAdminOrThrow(adminId));
    }

    public AdminProfileResponse updateProfile(UUID adminId, UpdateProfileRequest request) {
        Admin admin = findAdminOrThrow(adminId);
        admin.setFullName(request.fullName());
        admin.setPhone(request.phone());
        admin.setAvatarUrl(request.avatarUrl());
        adminRepository.save(admin);
        return adminMapper.toProfileResponse(admin);
    }

    private Admin findAdminOrThrow(UUID adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
    }

    private IssuedTokens issueTokens(Admin admin, String ipAddress, String userAgent) {
        String accessToken = jwtService.generateToken(admin.getEmail(), Map.of(
                "id", admin.getId().toString(),
                "role", admin.getRole().name()));

        String rawRefreshToken = tokenGenerator.generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .adminId(admin.getId())
                .tokenHash(tokenGenerator.hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(refreshTokenTtl))
                .createdByIp(ipAddress)
                .userAgent(userAgent)
                .createdAt(Instant.now())
                .build();
        entity = refreshTokenRepository.save(entity);

        return new IssuedTokens(accessToken, rawRefreshToken, entity);
    }

    private AuthResponse toAuthResponse(IssuedTokens issued, Admin admin) {
        return AuthResponse.bearer(
                issued.accessToken(),
                issued.rawRefreshToken(),
                jwtService.getExpirationSeconds(),
                adminMapper.toProfileResponse(admin));
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private record IssuedTokens(String accessToken, String rawRefreshToken, RefreshToken refreshTokenEntity) {
    }

}
