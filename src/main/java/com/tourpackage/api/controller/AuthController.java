package com.tourpackage.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tourpackage.api.dto.request.ForgotPasswordRequest;
import com.tourpackage.api.dto.request.LoginRequest;
import com.tourpackage.api.dto.request.RefreshTokenRequest;
import com.tourpackage.api.dto.request.ResetPasswordRequest;
import com.tourpackage.api.dto.request.UpdateProfileRequest;
import com.tourpackage.api.dto.response.AdminProfileResponse;
import com.tourpackage.api.dto.response.ApiResponse;
import com.tourpackage.api.dto.response.AuthResponse;
import com.tourpackage.api.security.AuthenticatedAdmin;
import com.tourpackage.api.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, clientIp(httpRequest), userAgent(httpRequest));
        return ApiResponse.of("Login successful", response);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(request, clientIp(httpRequest), userAgent(httpRequest));
        return ApiResponse.of("Token refreshed", response);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest httpRequest) {
        authService.forgotPassword(request, clientIp(httpRequest));
        return ApiResponse.message("If an account with that email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password has been reset. Please log in with your new password.");
    }

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ApiResponse.message("Email verified successfully. You can now log in.");
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AdminProfileResponse> getProfile(@AuthenticationPrincipal AuthenticatedAdmin principal) {
        return ApiResponse.of(authService.getProfile(principal.id()));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<AdminProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedAdmin principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.of("Profile updated", authService.updateProfile(principal.id(), request));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

}
