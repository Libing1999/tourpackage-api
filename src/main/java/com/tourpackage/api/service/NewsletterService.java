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
    private final MailService mailService;

    public NewsletterService(
            NewsletterSubscriberRepository newsletterSubscriberRepository,
            MailService mailService) {
        this.newsletterSubscriberRepository = newsletterSubscriberRepository;
        this.mailService = mailService;
    }

    /**
     * Idempotent: subscribing an already-active address is a silent no-op,
     * and resubscribing a previously-unsubscribed address reactivates it
     * rather than erroring, so the form never needs to distinguish "new"
     * from "welcome back."
     *
     * <p>The admin notification follows that same distinction rather than the
     * request: an already-subscribed address sends nothing, because someone
     * re-submitting the footer form twice is not news, and a form that mailed
     * the team on every submission would be trivial to turn into a flood.
     */
    public void subscribe(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        NotificationIntent intent = newsletterSubscriberRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existing -> {
                    if (existing.isActive()) {
                        return NotificationIntent.NONE;
                    }
                    existing.setActive(true);
                    existing.setSubscribedAt(Instant.now());
                    existing.setUnsubscribedAt(null);
                    newsletterSubscriberRepository.save(existing);
                    return NotificationIntent.REACTIVATED;
                })
                .orElseGet(() -> {
                    newsletterSubscriberRepository.save(NewsletterSubscriber.builder()
                            .email(normalizedEmail)
                            .active(true)
                            .subscribedAt(Instant.now())
                            .build());
                    return NotificationIntent.NEW;
                });

        if (intent != NotificationIntent.NONE) {
            mailService.sendNewsletterNotificationToAdmin(
                    normalizedEmail,
                    intent == NotificationIntent.REACTIVATED,
                    newsletterSubscriberRepository.countByActiveTrue());
        }
    }

    private enum NotificationIntent {
        NEW, REACTIVATED, NONE
    }

}
