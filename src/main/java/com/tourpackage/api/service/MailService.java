package com.tourpackage.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.tourpackage.api.dto.response.BookingResponse;
import com.tourpackage.api.dto.response.InquiryResponse;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;
    private final String adminNotificationAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.mail.admin-notifications}") String adminNotificationAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
        this.adminNotificationAddress = adminNotificationAddress;
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String rawToken) {
        String link = frontendUrl + "/admin/reset-password?token=" + rawToken;
        String html = """
                <p>Hi %s,</p>
                <p>We received a request to reset your TourPackage admin password. This link expires soon
                and can only be used once.</p>
                <p><a href="%s">Reset your password</a></p>
                <p>If you didn't request this, you can safely ignore this email.</p>
                """.formatted(fullName, link);

        send(toEmail, "Reset your TourPackage admin password", html);
    }

    @Async
    public void sendEmailVerificationEmail(String toEmail, String fullName, String rawToken) {
        String link = frontendUrl + "/admin/verify-email?token=" + rawToken;
        String html = """
                <p>Hi %s,</p>
                <p>Please verify your email address to activate your TourPackage admin account.</p>
                <p><a href="%s">Verify your email</a></p>
                """.formatted(fullName, link);

        send(toEmail, "Verify your TourPackage admin email", html);
    }

    @Async
    public void sendBookingConfirmationToCustomer(BookingResponse booking) {
        String html = """
                <p>Hi %s,</p>
                <p>Thanks for booking with TourPackage. We've received your request and it's now
                <strong>%s</strong> — we'll email you again as soon as it's confirmed.</p>
                <h3>Booking %s</h3>
                %s
                <p><a href="%s/bookings/%s">View your booking</a></p>
                <p>Questions? Just reply to this email.</p>
                """.formatted(
                booking.guestFullName(),
                booking.status(),
                booking.bookingNumber(),
                bookingDetailsTable(booking),
                frontendUrl,
                booking.bookingNumber());

        send(booking.guestEmail(), "Your TourPackage booking " + booking.bookingNumber(), html);
    }

    @Async
    public void sendBookingNotificationToAdmin(BookingResponse booking) {
        String html = """
                <p>A new booking has come in.</p>
                <h3>Booking %s</h3>
                %s
                <p><strong>Guest:</strong> %s &lt;%s&gt;%s</p>
                <p>It's currently <strong>%s</strong> and needs review.</p>
                """.formatted(
                booking.bookingNumber(),
                bookingDetailsTable(booking),
                booking.guestFullName(),
                booking.guestEmail(),
                booking.guestPhone() == null ? "" : " &middot; " + booking.guestPhone(),
                booking.status());

        String what = booking.hotelName() != null ? booking.hotelName() : booking.packageTitle();
        send(adminNotificationAddress, "New booking " + booking.bookingNumber() + " — " + what, html);
    }

    @Async
    public void sendBookingStatusUpdateToCustomer(BookingResponse booking) {
        String message = switch (booking.status()) {
            case CONFIRMED -> "Good news — your booking is now <strong>confirmed</strong>. We look forward to hosting you.";
            case CANCELLED -> "Your booking has been <strong>cancelled</strong>."
                    + (booking.cancellationReason() == null ? "" : " Reason: " + booking.cancellationReason());
            case COMPLETED -> "Your stay is complete. We hope you enjoyed it — we'd love to hear how it went.";
            case PENDING -> "Your booking is back to <strong>pending</strong> while we review it.";
        };

        String html = """
                <p>Hi %s,</p>
                <p>%s</p>
                <h3>Booking %s</h3>
                %s
                <p><a href="%s/bookings/%s">View your booking</a></p>
                """.formatted(
                booking.guestFullName(),
                message,
                booking.bookingNumber(),
                bookingDetailsTable(booking),
                frontendUrl,
                booking.bookingNumber());

        send(booking.guestEmail(), "Booking " + booking.bookingNumber() + " is " + booking.status(), html);
    }

    @Async
    public void sendInquiryNotificationToAdmin(InquiryResponse inquiry) {
        String html = """
                <p>A new enquiry has come in from the contact page.</p>
                <table cellpadding="6" style="border-collapse:collapse">
                  <tr><td><strong>Name</strong></td><td>%s</td></tr>
                  <tr><td><strong>Email</strong></td><td>%s</td></tr>
                  <tr><td><strong>Phone</strong></td><td>%s</td></tr>
                  <tr><td><strong>Travel date</strong></td><td>%s</td></tr>
                  <tr><td><strong>Party size</strong></td><td>%s</td></tr>
                  <tr><td><strong>About</strong></td><td>%s</td></tr>
                </table>
                <p><strong>Message</strong></p>
                <blockquote style="margin:0;padding-left:12px;border-left:3px solid #ddd">%s</blockquote>
                """.formatted(
                inquiry.name(),
                inquiry.email(),
                orDash(inquiry.phone()),
                inquiry.travelDate() == null ? "&mdash;" : inquiry.travelDate(),
                inquiry.partySize() == null ? "&mdash;" : inquiry.partySize(),
                inquiry.packageTitle() == null ? "General enquiry" : inquiry.packageTitle(),
                escape(inquiry.message()));

        send(adminNotificationAddress, "New enquiry from " + inquiry.name(), html);
    }

    @Async
    public void sendInquiryAcknowledgementToCustomer(InquiryResponse inquiry) {
        String html = """
                <p>Hi %s,</p>
                <p>Thanks for getting in touch with TourPackage. We've received your message and
                someone from the team will reply shortly.</p>
                <p><strong>What you sent us</strong></p>
                <blockquote style="margin:0;padding-left:12px;border-left:3px solid #ddd">%s</blockquote>
                <p>No need to reply to this email — it's just a confirmation.</p>
                """.formatted(inquiry.name(), escape(inquiry.message()));

        send(inquiry.email(), "We've received your message", html);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "&mdash;" : value;
    }

    /** The message is visitor-supplied and goes into an HTML email, so its
     * markup characters are escaped rather than rendered. */
    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");
    }

    /** What was booked, in a form that reads correctly for either type — a
     * package has a departure date and a duration where a hotel has check-in,
     * check-out, and a nightly rate. */
    private String bookingDetailsTable(BookingResponse booking) {
        String typeSpecificRows = switch (booking.bookingType()) {
            case HOTEL -> """
                      <tr><td><strong>Hotel</strong></td><td>%s, %s</td></tr>
                      <tr><td><strong>Room</strong></td><td>%s</td></tr>
                      <tr><td><strong>Check-in</strong></td><td>%s</td></tr>
                      <tr><td><strong>Check-out</strong></td><td>%s</td></tr>
                      <tr><td><strong>Nights</strong></td><td>%d</td></tr>
                    """.formatted(
                    booking.hotelName(), booking.cityName(),
                    booking.roomName(),
                    booking.startDate(), booking.endDate(),
                    booking.nights());
            case PACKAGE -> """
                      <tr><td><strong>Package</strong></td><td>%s</td></tr>
                      <tr><td><strong>Destination</strong></td><td>%s, %s</td></tr>
                      <tr><td><strong>Departs</strong></td><td>%s</td></tr>
                      <tr><td><strong>Returns</strong></td><td>%s</td></tr>
                      <tr><td><strong>Duration</strong></td><td>%d days / %d nights</td></tr>
                    """.formatted(
                    booking.packageTitle(),
                    booking.cityName(), booking.countryName(),
                    booking.startDate(), booking.endDate(),
                    booking.durationDays(), booking.durationNights());
        };

        return """
                <table cellpadding="6" style="border-collapse:collapse">
                %s  <tr><td><strong>Travellers</strong></td><td>%d adult(s), %d child(ren)</td></tr>
                  <tr><td><strong>Total</strong></td><td>%s %s</td></tr>
                </table>
                """.formatted(
                typeSpecificRows,
                booking.numberOfAdults(), booking.numberOfChildren(),
                booking.currencyCode(), booking.totalAmount());
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | RuntimeException ex) {
            // Mail delivery failures must not surface as request failures — the
            // token is already persisted, so the flow can be retried/resent.
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }

}
