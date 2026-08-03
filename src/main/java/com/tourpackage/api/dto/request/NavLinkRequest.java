package com.tourpackage.api.dto.request;

import com.tourpackage.api.entity.NavGroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NavLinkRequest(

        @NotNull(message = "Nav group is required")
        NavGroup navGroup,

        @NotBlank(message = "Label is required")
        @Size(max = 80, message = "Label must be at most 80 characters")
        String label,

        @NotBlank(message = "Link is required")
        @Size(max = 300, message = "Link must be at most 300 characters")
        String href,

        int displayOrder,

        boolean isActive

) {
}
