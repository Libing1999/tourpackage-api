package com.tourpackage.api.dto.request;

import com.tourpackage.api.entity.InquiryStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateInquiryStatusRequest(

        @NotNull(message = "Status is required")
        InquiryStatus status

) {
}
