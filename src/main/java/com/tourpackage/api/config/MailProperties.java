package com.tourpackage.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything under {@code app.mail}.
 *
 * <p>The rest of this codebase injects configuration with {@code @Value}, and for
 * one or two values that stays the clearer choice. Mail has nine, all of type
 * String — as constructor parameters they would be nine interchangeable
 * positions where transposing two compiles cleanly and silently sends every
 * customer email to the admin inbox. Binding them by name removes that
 * possibility entirely.
 *
 * @param enabled        whether to actually hand messages to the SMTP server
 * @param from           envelope sender
 * @param fromName       display name beside the sender, and the brand in templates
 * @param adminNotifications where booking/enquiry/subscriber alerts go
 * @param replyTo        where replies land, since {@code from} is a no-reply address
 * @param supportEmail   contact address shown in the footer
 * @param companyAddress optional postal address for the footer
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        boolean enabled,
        String from,
        String fromName,
        String adminNotifications,
        String replyTo,
        String supportEmail,
        String companyAddress) {
}
