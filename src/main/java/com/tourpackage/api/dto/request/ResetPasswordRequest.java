package com.tourpackage.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(

        @NotBlank(message = "Reset token is required")
        String token,

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).{8,100}$",
                message = "Password must be 8-100 characters and include an uppercase letter, "
                        + "a lowercase letter, a digit, and a special character (@$!%*?&#)"
        )
        String newPassword

) {
}
