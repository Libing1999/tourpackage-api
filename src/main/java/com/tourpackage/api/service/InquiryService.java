package com.tourpackage.api.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.CreateInquiryRequest;
import com.tourpackage.api.dto.response.InquiryAdminListResponse;
import com.tourpackage.api.dto.response.InquiryResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.Inquiry;
import com.tourpackage.api.entity.InquiryStatus;
import com.tourpackage.api.entity.TourPackage;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.InquiryRepository;
import com.tourpackage.api.repository.TourPackageRepository;

@Service
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final TourPackageRepository tourPackageRepository;
    private final MailService mailService;

    public InquiryService(
            InquiryRepository inquiryRepository,
            TourPackageRepository tourPackageRepository,
            MailService mailService) {
        this.inquiryRepository = inquiryRepository;
        this.tourPackageRepository = tourPackageRepository;
        this.mailService = mailService;
    }

    public InquiryResponse create(CreateInquiryRequest request) {
        // An unknown packageId is dropped rather than rejected: it only adds
        // context to the message, and failing a genuine enquiry because a
        // stale link pointed at a removed package would lose the enquiry for
        // no benefit to anyone.
        TourPackage tourPackage = request.packageId() == null
                ? null
                : tourPackageRepository.findByIdAndDeletedAtIsNull(request.packageId()).orElse(null);

        Instant now = Instant.now();
        Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
                .name(request.name().trim())
                .email(request.email().trim().toLowerCase())
                .phone(blankToNull(request.phone()))
                .travelDate(request.travelDate())
                .partySize(request.partySize())
                .message(request.message().trim())
                .packageId(tourPackage == null ? null : tourPackage.getId())
                .status(InquiryStatus.NEW)
                .createdAt(now)
                .updatedAt(now)
                .build());

        InquiryResponse response = toResponse(inquiry, tourPackage);

        // Both are @Async and swallow their own failures — a mail outage must
        // not lose an enquiry that's already saved.
        mailService.sendInquiryNotificationToAdmin(response);
        mailService.sendInquiryAcknowledgementToCustomer(response);

        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<InquiryAdminListResponse> list(InquiryStatus status, String search, Pageable pageable) {
        return PageResponse.of(inquiryRepository.searchAdmin(status, search, pageable));
    }

    @Transactional(readOnly = true)
    public InquiryResponse getById(UUID id) {
        Inquiry inquiry = findOrThrow(id);
        return toResponse(inquiry, findPackage(inquiry.getPackageId()));
    }

    public InquiryResponse updateStatus(UUID id, InquiryStatus status) {
        Inquiry inquiry = findOrThrow(id);

        inquiry.setStatus(status);
        // respondedAt marks the first time this left the NEW pile — it's a
        // "when did we get back to them", so it isn't cleared on later moves.
        if (status != InquiryStatus.NEW && inquiry.getRespondedAt() == null) {
            inquiry.setRespondedAt(Instant.now());
        }
        inquiry.setUpdatedAt(Instant.now());

        inquiryRepository.save(inquiry);
        return toResponse(inquiry, findPackage(inquiry.getPackageId()));
    }

    private InquiryResponse toResponse(Inquiry inquiry, TourPackage tourPackage) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getName(),
                inquiry.getEmail(),
                inquiry.getPhone(),
                inquiry.getTravelDate(),
                inquiry.getPartySize(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getPackageId(),
                tourPackage == null ? null : tourPackage.getTitle(),
                tourPackage == null ? null : tourPackage.getSlug(),
                inquiry.getRespondedAt(),
                inquiry.getCreatedAt());
    }

    private TourPackage findPackage(UUID packageId) {
        return packageId == null ? null : tourPackageRepository.findById(packageId).orElse(null);
    }

    private Inquiry findOrThrow(UUID id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
