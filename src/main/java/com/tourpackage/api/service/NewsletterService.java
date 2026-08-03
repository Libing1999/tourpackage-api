package com.tourpackage.api.service;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.entity.NewsletterSubscriber;
import com.tourpackage.api.repository.NewsletterSubscriberRepository;

@Service
@Transactional
public class NewsletterService {

    private final NewsletterSubscriberRepository newsletterSubscriberRepository;

    public NewsletterService(NewsletterSubscriberRepository newsletterSubscriberRepository) {
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
    }

    /**
     * Idempotent: subscribing an already-active address is a silent no-op,
     * and resubscribing a previously-unsubscribed address reactivates it
     * rather than erroring, so the form never needs to distinguish "new"
     * from "welcome back."
     */
    public void subscribe(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        newsletterSubscriberRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresentOrElse(
                        existing -> {
                            if (!existing.isActive()) {
                                existing.setActive(true);
                                existing.setSubscribedAt(Instant.now());
                                existing.setUnsubscribedAt(null);
                                newsletterSubscriberRepository.save(existing);
                            }
                        },
                        () -> newsletterSubscriberRepository.save(NewsletterSubscriber.builder()
                                .email(normalizedEmail)
                                .active(true)
                                .subscribedAt(Instant.now())
                                .build()));
    }

}
