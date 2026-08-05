package com.tourpackage.api.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.config.CacheConfig;
import com.tourpackage.api.dto.request.FaqRequest;
import com.tourpackage.api.dto.request.TestimonialRequest;
import com.tourpackage.api.dto.response.CustomerResponse;
import com.tourpackage.api.dto.response.FaqAdminResponse;
import com.tourpackage.api.dto.response.NewsletterSubscriberResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.dto.response.SettingResponse;
import com.tourpackage.api.dto.response.TestimonialAdminResponse;
import com.tourpackage.api.entity.Faq;
import com.tourpackage.api.entity.NewsletterSubscriber;
import com.tourpackage.api.entity.Setting;
import com.tourpackage.api.entity.Testimonial;
import com.tourpackage.api.exception.ApiException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.CountryRepository;
import com.tourpackage.api.repository.FaqRepository;
import com.tourpackage.api.repository.NewsletterSubscriberRepository;
import com.tourpackage.api.repository.SettingRepository;
import com.tourpackage.api.repository.TestimonialRepository;
import com.tourpackage.api.repository.TourPackageRepository;
import com.tourpackage.api.repository.UserRepository;

/**
 * The back-office modules that are each small enough not to warrant a service
 * of their own — customers, newsletter, testimonials, FAQs, settings. Hotels,
 * packages, bookings and enquiries have their own services because each
 * carries real domain rules; these are mostly list-and-edit.
 */
@Service
@Transactional
public class AdminContentService {

    private final UserRepository userRepository;
    private final NewsletterSubscriberRepository newsletterSubscriberRepository;
    private final TestimonialRepository testimonialRepository;
    private final FaqRepository faqRepository;
    private final SettingRepository settingRepository;
    private final CountryRepository countryRepository;
    private final TourPackageRepository tourPackageRepository;

    public AdminContentService(
            UserRepository userRepository,
            NewsletterSubscriberRepository newsletterSubscriberRepository,
            TestimonialRepository testimonialRepository,
            FaqRepository faqRepository,
            SettingRepository settingRepository,
            CountryRepository countryRepository,
            TourPackageRepository tourPackageRepository) {
        this.userRepository = userRepository;
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.testimonialRepository = testimonialRepository;
        this.faqRepository = faqRepository;
        this.settingRepository = settingRepository;
        this.countryRepository = countryRepository;
        this.tourPackageRepository = tourPackageRepository;
    }

    // --- Customers -------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> listCustomers(String search, Pageable pageable) {
        return PageResponse.of(userRepository.searchCustomers(search, pageable));
    }

    // --- Newsletter ------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<NewsletterSubscriberResponse> listSubscribers(Boolean active, Pageable pageable) {
        var page = active == null
                ? newsletterSubscriberRepository.findAllByOrderBySubscribedAtDesc(pageable)
                : newsletterSubscriberRepository.findByActiveOrderBySubscribedAtDesc(active, pageable);

        return PageResponse.of(page.map(s -> new NewsletterSubscriberResponse(
                s.getId(), s.getEmail(), s.isActive(), s.getSubscribedAt(), s.getUnsubscribedAt())));
    }

    /** Removing a subscriber deactivates rather than deletes, so a later
     * re-subscribe reactivates the same row and the audit trail survives. */
    public void unsubscribe(UUID id) {
        NewsletterSubscriber subscriber = newsletterSubscriberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscriber not found: " + id));

        if (!subscriber.isActive()) {
            throw new ApiException(HttpStatus.CONFLICT, "This subscriber is already unsubscribed");
        }

        subscriber.setActive(false);
        subscriber.setUnsubscribedAt(Instant.now());
        newsletterSubscriberRepository.save(subscriber);
    }

    // --- Testimonials ----------------------------------------------------

    @Transactional(readOnly = true)
    public List<TestimonialAdminResponse> listTestimonials() {
        return testimonialRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toTestimonialResponse)
                .toList();
    }

    public TestimonialAdminResponse createTestimonial(TestimonialRequest request) {
        validateTestimonialReferences(request);

        Instant now = Instant.now();
        Testimonial testimonial = Testimonial.builder()
                .customerName(request.customerName().trim())
                .customerAvatarUrl(request.customerAvatarUrl())
                .customerCountryId(request.customerCountryId())
                .packageId(request.packageId())
                .rating(request.rating())
                .message(request.message().trim())
                .featured(request.isFeatured())
                .active(request.isActive())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toTestimonialResponse(testimonialRepository.save(testimonial));
    }

    public TestimonialAdminResponse updateTestimonial(UUID id, TestimonialRequest request) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found: " + id));
        validateTestimonialReferences(request);

        testimonial.setCustomerName(request.customerName().trim());
        testimonial.setCustomerAvatarUrl(request.customerAvatarUrl());
        testimonial.setCustomerCountryId(request.customerCountryId());
        testimonial.setPackageId(request.packageId());
        testimonial.setRating(request.rating());
        testimonial.setMessage(request.message().trim());
        testimonial.setFeatured(request.isFeatured());
        testimonial.setActive(request.isActive());
        testimonial.setUpdatedAt(Instant.now());

        return toTestimonialResponse(testimonialRepository.save(testimonial));
    }

    public void deleteTestimonial(UUID id) {
        Testimonial testimonial = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found: " + id));
        testimonialRepository.delete(testimonial);
    }

    // --- FAQs ------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<FaqAdminResponse> listFaqs() {
        return faqRepository.findAll().stream()
                .sorted((a, b) -> {
                    int byCategory = a.getCategory().compareToIgnoreCase(b.getCategory());
                    return byCategory != 0 ? byCategory : Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                })
                .map(this::toFaqResponse)
                .toList();
    }

    @CacheEvict(value = CacheConfig.FAQS, allEntries = true)
    public FaqAdminResponse createFaq(FaqRequest request) {
        Instant now = Instant.now();
        Faq faq = Faq.builder()
                .question(request.question().trim())
                .answer(request.answer().trim())
                .category(request.category().trim())
                .displayOrder(request.displayOrder())
                .active(request.isActive())
                .createdAt(now)
                .updatedAt(now)
                .build();

        return toFaqResponse(faqRepository.save(faq));
    }

    @CacheEvict(value = CacheConfig.FAQS, allEntries = true)
    public FaqAdminResponse updateFaq(UUID id, FaqRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));

        faq.setQuestion(request.question().trim());
        faq.setAnswer(request.answer().trim());
        faq.setCategory(request.category().trim());
        faq.setDisplayOrder(request.displayOrder());
        faq.setActive(request.isActive());
        faq.setUpdatedAt(Instant.now());

        return toFaqResponse(faqRepository.save(faq));
    }

    @CacheEvict(value = CacheConfig.FAQS, allEntries = true)
    public void deleteFaq(UUID id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));
        faqRepository.delete(faq);
    }

    // --- Settings --------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SettingResponse> listSettings() {
        return settingRepository.findAll().stream()
                .sorted((a, b) -> {
                    int byGroup = a.getGroupName().compareToIgnoreCase(b.getGroupName());
                    return byGroup != 0 ? byGroup : a.getKey().compareToIgnoreCase(b.getKey());
                })
                .map(s -> new SettingResponse(
                        s.getId(), s.getKey(), s.getValue(), s.getValueType(), s.getGroupName(), s.isVisible()))
                .toList();
    }

    @CacheEvict(value = CacheConfig.SETTINGS, allEntries = true)
    public List<SettingResponse> updateSettings(Map<String, String> values, UUID updatedBy) {
        List<Setting> all = settingRepository.findAll();

        // Reject the whole request if any key is unknown, rather than applying
        // the recognised half — a partial save on a settings screen leaves the
        // admin with no way to tell what actually stuck.
        List<String> unknown = values.keySet().stream()
                .filter(key -> all.stream().noneMatch(s -> s.getKey().equals(key)))
                .toList();

        if (!unknown.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown setting(s): " + String.join(", ", unknown));
        }

        Instant now = Instant.now();
        all.stream()
                .filter(setting -> values.containsKey(setting.getKey()))
                .forEach(setting -> {
                    setting.setValue(values.get(setting.getKey()));
                    setting.setUpdatedBy(updatedBy);
                    setting.setUpdatedAt(now);
                });

        settingRepository.saveAll(all);
        return listSettings();
    }

    // --- helpers ---------------------------------------------------------

    private void validateTestimonialReferences(TestimonialRequest request) {
        if (request.customerCountryId() != null && !countryRepository.existsById(request.customerCountryId())) {
            throw new ResourceNotFoundException("Country not found: " + request.customerCountryId());
        }
        if (request.packageId() != null && !tourPackageRepository.existsById(request.packageId())) {
            throw new ResourceNotFoundException("Package not found: " + request.packageId());
        }
    }

    private TestimonialAdminResponse toTestimonialResponse(Testimonial t) {
        return new TestimonialAdminResponse(
                t.getId(),
                t.getCustomerName(),
                t.getCustomerAvatarUrl(),
                t.getCustomerCountryId(),
                t.getCustomerCountryId() == null ? null
                        : countryRepository.findById(t.getCustomerCountryId())
                                .map(c -> c.getName()).orElse(null),
                t.getPackageId(),
                t.getPackageId() == null ? null
                        : tourPackageRepository.findById(t.getPackageId())
                                .map(p -> p.getTitle()).orElse(null),
                t.getRating(),
                t.getMessage(),
                t.isFeatured(),
                t.isActive(),
                t.getCreatedAt());
    }

    private FaqAdminResponse toFaqResponse(Faq f) {
        return new FaqAdminResponse(
                f.getId(), f.getQuestion(), f.getAnswer(), f.getCategory(),
                f.getDisplayOrder(), f.isActive(), f.getCreatedAt());
    }

}
