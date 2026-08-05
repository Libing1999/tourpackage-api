package com.tourpackage.api.service;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tourpackage.api.config.MailProperties;
import com.tourpackage.api.dto.response.BookingResponse;
import com.tourpackage.api.dto.response.InquiryResponse;
import com.tourpackage.api.service.EmailTemplateEngine.Raw;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Every email the application sends.
 *
 * <p>Two audiences. The <strong>admin</strong> team is notified when a hotel or
 * tour is booked, when the contact form is submitted, and when someone
 * subscribes to the newsletter. The <strong>customer</strong> gets a booking
 * confirmation, an enquiry acknowledgement, and an update whenever a booking
 * changes status.
 *
 * <p>All sending is {@code @Async}: SMTP is a network call to a third party, and
 * a customer submitting a booking form should not wait on it — nor should a
 * timeout there fail a booking that is already committed.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("d MMM yyyy 'at' HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    /**
     * Dates are written out rather than left as ISO. {@code 2026-11-14} is how
     * the API stores a date; {@code Sat, 14 Nov 2026} is how someone reads their
     * own check-in date. The weekday earns its place here — travellers care
     * whether a departure is a Saturday.
     */
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final EmailTemplateEngine templates;
    private final MailProperties mail;
    private final String frontendUrl;
    private final long passwordResetExpiryMinutes;
    private final long emailVerificationExpiryHours;

    public MailService(
            JavaMailSender mailSender,
            EmailTemplateEngine templates,
            MailProperties mail,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.password-reset.token-expiration-minutes}") long passwordResetExpiryMinutes,
            @Value("${app.email-verification.token-expiration-hours}") long emailVerificationExpiryHours) {
        this.mailSender = mailSender;
        this.templates = templates;
        this.mail = mail;
        this.frontendUrl = frontendUrl;
        this.passwordResetExpiryMinutes = passwordResetExpiryMinutes;
        this.emailVerificationExpiryHours = emailVerificationExpiryHours;
    }

    // ---------------------------------------------------------------- admin

    @Async
    public void sendBookingNotificationToAdmin(BookingResponse booking) {
        String what = booking.hotelName() != null ? booking.hotelName() : booking.packageTitle();
        String type = booking.bookingType() == com.tourpackage.api.entity.BookingType.HOTEL ? "hotel" : "tour";

        String html = templates.render("booking-notification", Map.of(
                "bookingNumber", booking.bookingNumber(),
                "bookingType", type,
                "status", status(booking.status()),
                "detailsTable", new Raw(bookingDetailsTable(booking)),
                "guestName", booking.guestFullName(),
                "guestEmail", booking.guestEmail(),
                "guestPhone", orDash(booking.guestPhone()),
                "adminUrl", frontendUrl + "/dashboard/bookings"),
                layout("New " + type + " booking from " + booking.guestFullName(),
                        "New " + type + " booking " + booking.bookingNumber() + " — " + what));

        send(mail.adminNotifications(), "New booking " + booking.bookingNumber() + " — " + what, html);
    }

    @Async
    public void sendInquiryNotificationToAdmin(InquiryResponse inquiry) {
        String html = templates.render("inquiry-notification", Map.of(
                "name", inquiry.name(),
                "email", inquiry.email(),
                "phone", orDash(inquiry.phone()),
                "travelDate", date(inquiry.travelDate()),
                "partySize", inquiry.partySize() == null ? "—" : inquiry.partySize(),
                "about", inquiry.packageTitle() == null ? "General enquiry" : inquiry.packageTitle(),
                "message", new Raw(paragraphs(inquiry.message()))),
                layout("New enquiry from " + inquiry.name(),
                        inquiry.name() + " submitted the contact form"));

        send(mail.adminNotifications(), "New enquiry from " + inquiry.name(), html);
    }

    /**
     * @param reactivated a previously-unsubscribed address coming back, which is
     *                    worth distinguishing from a first-time signup
     */
    @Async
    public void sendNewsletterNotificationToAdmin(String email, boolean reactivated, long totalActive) {
        String html = templates.render("newsletter-notification", Map.of(
                "email", email,
                "subscriptionKind", reactivated ? "re-subscribed to" : "subscribed to",
                "subscribedAt", TIMESTAMP.format(Instant.now()),
                "totalActive", totalActive,
                "adminUrl", frontendUrl + "/dashboard/newsletter"),
                layout("New newsletter subscriber", email + " joined the newsletter"));

        send(mail.adminNotifications(), "New newsletter subscriber — " + email, html);
    }

    // ------------------------------------------------------------- customer

    @Async
    public void sendBookingConfirmationToCustomer(BookingResponse booking) {
        String html = templates.render("booking-confirmation", Map.of(
                "guestName", booking.guestFullName(),
                "status", status(booking.status()),
                "bookingNumber", booking.bookingNumber(),
                "detailsTable", new Raw(bookingDetailsTable(booking)),
                "bookingUrl", frontendUrl + "/bookings/" + booking.bookingNumber()),
                layout("Your booking " + booking.bookingNumber(),
                        "We've got your booking — reference " + booking.bookingNumber()));

        send(booking.guestEmail(), "Your " + mail.fromName() + " booking " + booking.bookingNumber(), html);
    }

    @Async
    public void sendBookingStatusUpdateToCustomer(BookingResponse booking) {
        String message = switch (booking.status()) {
            case CONFIRMED -> "Good news — your booking is now <strong style=\"color:#18181b;\">confirmed</strong>. "
                    + "We look forward to hosting you.";
            case CANCELLED -> "Your booking has been <strong style=\"color:#18181b;\">cancelled</strong>."
                    + (booking.cancellationReason() == null
                            ? ""
                            : " Reason: " + EmailTemplateEngine.escapeHtml(booking.cancellationReason()));
            case COMPLETED -> "Your trip is complete. We hope you enjoyed it — we'd love to hear how it went.";
            case PENDING -> "Your booking is back to <strong style=\"color:#18181b;\">pending</strong> while we review it.";
        };

        String html = templates.render("booking-status-update", Map.of(
                "guestName", booking.guestFullName(),
                "status", status(booking.status()),
                "bookingNumber", booking.bookingNumber(),
                "statusMessage", new Raw(message),
                "detailsTable", new Raw(bookingDetailsTable(booking)),
                "bookingUrl", frontendUrl + "/bookings/" + booking.bookingNumber()),
                layout("Booking " + booking.bookingNumber() + " is " + status(booking.status()),
                        "Your booking is now " + status(booking.status())));

        send(booking.guestEmail(), "Booking " + booking.bookingNumber() + " is " + status(booking.status()), html);
    }

    @Async
    public void sendInquiryAcknowledgementToCustomer(InquiryResponse inquiry) {
        String html = templates.render("inquiry-acknowledgement", Map.of(
                "name", inquiry.name(),
                "message", new Raw(paragraphs(inquiry.message()))),
                layout("We've received your message", "Thanks for getting in touch — we'll reply shortly"));

        send(inquiry.email(), "We've received your message", html);
    }

    // ---------------------------------------------------------------- admin accounts

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String rawToken) {
        String link = frontendUrl + "/reset-password?token=" + rawToken;
        String html = templates.render("password-reset", Map.of(
                "fullName", fullName,
                "brandName", mail.fromName(),
                "link", link,
                "expiryMinutes", passwordResetExpiryMinutes),
                layout("Reset your password", "Reset the password on your admin account"));

        send(toEmail, "Reset your " + mail.fromName() + " admin password", html);
    }

    @Async
    public void sendEmailVerificationEmail(String toEmail, String fullName, String rawToken) {
        String link = frontendUrl + "/verify-email?token=" + rawToken;
        String html = templates.render("email-verification", Map.of(
                "fullName", fullName,
                "brandName", mail.fromName(),
                "link", link,
                "expiryHours", emailVerificationExpiryHours),
                layout("Verify your email", "Confirm your address to activate your admin account"));

        send(toEmail, "Verify your " + mail.fromName() + " admin email", html);
    }

    // ---------------------------------------------------------------- internals

    /** Values the shared layout needs, identical for every message. */
    private Map<String, Object> layout(String subject, String preheader) {
        return Map.of(
                "subject", subject,
                "preheader", preheader,
                "brandName", mail.fromName(),
                "siteUrl", frontendUrl,
                "supportEmail", mail.supportEmail(),
                "footerAddress", mail.companyAddress() == null || mail.companyAddress().isBlank()
                        ? ""
                        : " · " + mail.companyAddress());
    }

    /**
     * A booking status as prose.
     *
     * <p>The enum constant is {@code CANCELLED}, which set into a sentence gives
     * "your booking is CANCELLED" — shouting at someone whose trip just fell
     * through. Every use of it here sits mid-sentence or in a heading, so it is
     * lowercased at the one place it crosses into readable text.
     */
    private static String status(com.tourpackage.api.entity.BookingStatus status) {
        return status.name().toLowerCase(Locale.ENGLISH);
    }

    /** A date as a reader would write it, or a dash when there isn't one. */
    private static String date(java.time.LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /**
     * Visitor-supplied text as HTML paragraphs.
     *
     * <p>Escaped first, then line breaks are turned into markup — the other order
     * would escape the {@code <br />} tags this method just added.
     */
    private static String paragraphs(String text) {
        return EmailTemplateEngine.escapeHtml(text).replace("\n", "<br />");
    }

    /** What was booked, in a form that reads correctly for either type — a
     * package has a departure date and a duration where a hotel has check-in,
     * check-out, and a nightly rate. */
    private String bookingDetailsTable(BookingResponse booking) {
        String typeSpecificRows = switch (booking.bookingType()) {
            case HOTEL -> row("Hotel", booking.hotelName() + ", " + booking.cityName())
                    + row("Room", booking.roomName())
                    + row("Check-in", date(booking.startDate()))
                    + row("Check-out", date(booking.endDate()))
                    + row("Nights", String.valueOf(booking.nights()));
            case PACKAGE -> row("Package", booking.packageTitle())
                    + row("Destination", booking.cityName() + ", " + booking.countryName())
                    + row("Departs", date(booking.startDate()))
                    + row("Returns", date(booking.endDate()))
                    + row("Duration", booking.durationDays() + " days / " + booking.durationNights() + " nights");
        };

        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" \
                style="border-collapse:collapse;font-family:Helvetica,Arial,sans-serif;font-size:14px;">
                %s%s%s</table>
                """.formatted(
                typeSpecificRows,
                row("Travellers", booking.numberOfAdults() + " adult(s), " + booking.numberOfChildren() + " child(ren)"),
                totalRow(booking.currencyCode() + " " + booking.totalAmount()));
    }

    private static String row(String label, String value) {
        return """
                  <tr>
                    <td style="padding:8px 12px;border:1px solid #e4e4e7;background-color:#fafafa;color:#71717a;width:35%%;">%s</td>
                    <td style="padding:8px 12px;border:1px solid #e4e4e7;color:#18181b;">%s</td>
                  </tr>
                """.formatted(EmailTemplateEngine.escapeHtml(label), EmailTemplateEngine.escapeHtml(value));
    }

    private static String totalRow(String value) {
        return """
                  <tr>
                    <td style="padding:8px 12px;border:1px solid #e4e4e7;background-color:#fafafa;color:#71717a;font-weight:bold;">Total</td>
                    <td style="padding:8px 12px;border:1px solid #e4e4e7;color:#18181b;font-weight:bold;font-size:16px;">%s</td>
                  </tr>
                """.formatted(EmailTemplateEngine.escapeHtml(value));
    }

    private void send(String to, String subject, String html) {
        if (!mail.enabled()) {
            log.info("Mail disabled (app.mail.enabled=false) — would have sent '{}' to {}", subject, to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart/alternative: the HTML part is what almost everyone sees,
            // but a text-only client would otherwise be shown raw markup, and a
            // message with no text part scores worse with spam filters.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mail.from(), mail.fromName());
            helper.setTo(to);
            helper.setReplyTo(mail.replyTo());
            helper.setSubject(subject);
            helper.setText(templates.toPlainText(html), html);
            mailSender.send(message);
            log.debug("Sent '{}' to {}", subject, to);
        } catch (MessagingException | UnsupportedEncodingException | RuntimeException ex) {
            // Delivery failures must not surface as request failures — the booking
            // or enquiry is already committed, and losing it because an SMTP host
            // was briefly unreachable would be far worse than a missing email.
            log.error("Failed to send '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }

}
