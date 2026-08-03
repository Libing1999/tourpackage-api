package com.tourpackage.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tourpackage.api.entity.NewsletterSubscriber;

public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, UUID> {

    Optional<NewsletterSubscriber> findByEmailIgnoreCase(String email);

    long countByActiveTrue();

    Page<NewsletterSubscriber> findAllByOrderBySubscribedAtDesc(Pageable pageable);

    Page<NewsletterSubscriber> findByActiveOrderBySubscribedAtDesc(boolean active, Pageable pageable);

}
