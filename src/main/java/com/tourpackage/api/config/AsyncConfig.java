package com.tourpackage.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} so mail sending doesn't block the request thread
 * on SMTP round-trip latency (forgot-password / email-verification).
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
