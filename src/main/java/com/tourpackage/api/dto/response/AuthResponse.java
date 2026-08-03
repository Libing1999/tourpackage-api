package com.tourpackage.api.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        AdminProfileResponse admin
) {

    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresIn, AdminProfileResponse admin) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, admin);
    }

}
