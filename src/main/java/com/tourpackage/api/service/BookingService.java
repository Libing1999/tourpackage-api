package com.tourpackage.api.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourpackage.api.dto.request.CreateHotelBookingRequest;
import com.tourpackage.api.dto.request.CreatePackageBookingRequest;
import com.tourpackage.api.dto.request.GuestDetailsRequest;
import com.tourpackage.api.dto.request.TravellerRequest;
import com.tourpackage.api.dto.response.BookingAdminListResponse;
import com.tourpackage.api.dto.response.BookingPaymentResponse;
import com.tourpackage.api.dto.response.BookingResponse;
import com.tourpackage.api.dto.response.BookingTravellerResponse;
import com.tourpackage.api.dto.response.PageResponse;
import com.tourpackage.api.entity.Booking;
import com.tourpackage.api.entity.BookingPayment;
import com.tourpackage.api.entity.BookingStatus;
import com.tourpackage.api.entity.BookingTraveller;
import com.tourpackage.api.entity.BookingType;
import com.tourpackage.api.entity.City;
import com.tourpackage.api.entity.ContentStatus;
import com.tourpackage.api.entity.Country;
import com.tourpackage.api.entity.Hotel;
import com.tourpackage.api.entity.HotelRoom;
import com.tourpackage.api.entity.PaymentMethod;
import com.tourpackage.api.entity.PaymentStatus;
import com.tourpackage.api.entity.RoomType;
import com.tourpackage.api.entity.TourPackage;
import com.tourpackage.api.entity.User;
import com.tourpackage.api.entity.UserStatus;
import com.tourpackage.api.exception.ApiException;
import com.tourpackage.api.exception.ResourceNotFoundException;
import com.tourpackage.api.repository.BookingPaymentRepository;
import com.tourpackage.api.repository.BookingRepository;
import com.tourpackage.api.repository.BookingTravellerRepository;
import com.tourpackage.api.repository.CityRepository;
import com.tourpackage.api.repository.CountryRepository;
import com.tourpackage.api.repository.HotelRepository;
import com.tourpackage.api.repository.HotelRoomRepository;
import com.tourpackage.api.repository.RoomTypeRepository;
import com.tourpackage.api.repository.TourPackageRepository;
import com.tourpackage.api.repository.UserRepository;

@Service
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingTravellerRepository bookingTravellerRepository;
    private final BookingPaymentRepository bookingPaymentRepository;
    private final UserRepository userRepository;
    private final HotelRoomRepository hotelRoomRepository;
    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final TourPackageRepository tourPackageRepository;
    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;
    private final PackagePricing packagePricing;
    private final MailService mailService;

    public BookingService(
            BookingRepository bookingRepository,
            BookingTravellerRepository bookingTravellerRepository,
            BookingPaymentRepository bookingPaymentRepository,
            UserRepository userRepository,
            HotelRoomRepository hotelRoomRepository,
            HotelRepository hotelRepository,
            RoomTypeRepository roomTypeRepository,
            TourPackageRepository tourPackageRepository,
            CityRepository cityRepository,
            CountryRepository countryRepository,
            PackagePricing packagePricing,
            MailService mailService) {
        this.bookingRepository = bookingRepository;
        this.bookingTravellerRepository = bookingTravellerRepository;
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.userRepository = userRepository;
        this.hotelRoomRepository = hotelRoomRepository;
        this.hotelRepository = hotelRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.tourPackageRepository = tourPackageRepository;
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
        this.packagePricing = packagePricing;
        this.mailService = mailService;
    }

    public BookingResponse createHotelBooking(CreateHotelBookingRequest request) {
        HotelRoom room = hotelRoomRepository.findById(request.hotelRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.hotelRoomId()));

        int nights = validateStay(request, room);
        BigDecimal totalAmount = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        User guest = findOrCreateGuest(request.guest());

        Instant now = Instant.now();
        Booking booking = bookingRepository.save(Booking.builder()
                .userId(guest.getId())
                .bookingType(BookingType.HOTEL)
                .hotelRoomId(room.getId())
                .travelDate(request.checkInDate())
                .returnDate(request.checkOutDate())
                .numberOfAdults(request.numberOfAdults())
                .numberOfChildren(request.numberOfChildren())
                .totalAmount(totalAmount)
                .currencyCode(room.getCurrencyCode())
                .status(BookingStatus.PENDING)
                .specialRequests(request.specialRequests())
                .createdAt(now)
                .updatedAt(now)
                .build());

        saveTravellers(booking.getId(), request.travellers(), now);
        recordPlaceholderPayment(booking, request.paymentMethod(), now);

        BookingResponse response = buildResponse(booking);

        // Both are @Async and swallow their own failures — a mail outage must
        // not roll back a booking the guest has already been charged for (in
        // the sense of having committed to).
        mailService.sendBookingConfirmationToCustomer(response);
        mailService.sendBookingNotificationToAdmin(response);

        return response;
    }

    public BookingResponse createPackageBooking(CreatePackageBookingRequest request) {
        TourPackage tourPackage = tourPackageRepository
                .findByIdAndDeletedAtIsNull(request.packageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + request.packageId()));

        validatePackageBooking(request, tourPackage);

        BigDecimal totalAmount = packagePricing.total(
                tourPackage, request.numberOfAdults(), request.numberOfChildren());

        // A package's length is fixed, so the return date is derived rather
        // than asked for: a 7-day trip departing the 10th returns on the 16th.
        LocalDate returnDate = request.travelDate().plusDays(tourPackage.getDurationDays() - 1L);

        User guest = findOrCreateGuest(request.guest());

        Instant now = Instant.now();
        Booking booking = bookingRepository.save(Booking.builder()
                .userId(guest.getId())
                .bookingType(BookingType.PACKAGE)
                .packageId(tourPackage.getId())
                .travelDate(request.travelDate())
                .returnDate(returnDate)
                .numberOfAdults(request.numberOfAdults())
                .numberOfChildren(request.numberOfChildren())
                .totalAmount(totalAmount)
                .currencyCode(tourPackage.getCurrencyCode())
                .status(BookingStatus.PENDING)
                .specialRequests(request.specialRequests())
                .createdAt(now)
                .updatedAt(now)
                .build());

        saveTravellers(booking.getId(), request.travellers(), now);
        recordPlaceholderPayment(booking, request.paymentMethod(), now);

        BookingResponse response = buildResponse(booking);

        mailService.sendBookingConfirmationToCustomer(response);
        mailService.sendBookingNotificationToAdmin(response);

        return response;
    }

    /**
     * Booking history for a guest who has no account. Listing by email alone
     * would let anyone enumerate someone else's trips, so the caller has to
     * prove they hold one valid reference for that address first — then they
     * get everything booked with it. Same "prove one, see all" shape a
     * "manage my booking" page uses when there's no login to lean on.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getHistory(String bookingNumber, String email) {
        // Reuses the single-booking lookup, so the ownership check and its
        // deliberately-generic 404 live in exactly one place.
        BookingResponse verified = getByBookingNumber(bookingNumber, email);

        User guest = userRepository.findByEmailAndDeletedAtIsNull(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingNumber));

        return bookingRepository.findByUserIdOrderByCreatedAtDesc(guest.getId()).stream()
                .map(booking -> booking.getBookingNumber().equals(verified.bookingNumber())
                        ? verified
                        : buildResponse(booking))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getByBookingNumber(String bookingNumber, String email) {
        Booking booking = bookingRepository.findByBookingNumber(bookingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingNumber));

        // A booking number alone is guessable (it's a sequence), so the lookup
        // is gated on also knowing the email it was made with. Same generic
        // 404 either way — confirming that a number exists but the email is
        // wrong would leak which numbers are real.
        User guest = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingNumber));

        if (!guest.getEmail().equals(normalizeEmail(email))) {
            throw new ResourceNotFoundException("Booking not found: " + bookingNumber);
        }

        return buildResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingAdminListResponse> list(BookingStatus status, String search, Pageable pageable) {
        return PageResponse.of(bookingRepository.searchAdmin(status, search, pageable));
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(UUID id) {
        return buildResponse(findBookingOrThrow(id));
    }

    public BookingResponse updateStatus(UUID id, BookingStatus newStatus, String cancellationReason) {
        Booking booking = findBookingOrThrow(id);
        BookingStatus current = booking.getStatus();

        if (current == newStatus) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking is already " + newStatus);
        }
        if (current == BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "A cancelled booking cannot change status");
        }
        if (current == BookingStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "A completed booking cannot change status");
        }
        if (newStatus == BookingStatus.COMPLETED && current != BookingStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only a confirmed booking can be marked completed");
        }

        booking.setStatus(newStatus);

        // ck_bookings_cancellation requires cancelled_at to be set if and only
        // if the status is CANCELLED, so both directions have to be handled.
        if (newStatus == BookingStatus.CANCELLED) {
            booking.setCancelledAt(Instant.now());
            booking.setCancellationReason(cancellationReason);
        } else {
            booking.setCancelledAt(null);
            booking.setCancellationReason(null);
        }

        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        BookingResponse response = buildResponse(booking);
        mailService.sendBookingStatusUpdateToCustomer(response);
        return response;
    }

    /**
     * Validates the requested stay against the room and returns its length in
     * nights. Every rule here is also a DB constraint or a business fact — the
     * point of checking first is to answer with a 400/409 that names the
     * problem instead of a constraint violation the guest can't act on.
     */
    private int validateStay(CreateHotelBookingRequest request, HotelRoom room) {
        LocalDate checkIn = request.checkInDate();
        LocalDate checkOut = request.checkOutDate();

        if (!checkOut.isAfter(checkIn)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Check-out date must be after the check-in date");
        }

        if (!room.isAvailable()) {
            throw new ApiException(HttpStatus.CONFLICT, "This room is not currently available to book");
        }

        if (request.numberOfAdults() > room.getMaxAdults()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This room sleeps at most " + room.getMaxAdults() + " adult(s)");
        }
        if (request.numberOfChildren() > room.getMaxChildren()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This room allows at most " + room.getMaxChildren() + " child(ren)");
        }

        long alreadyBooked = bookingRepository.countOverlapping(room.getId(), checkIn, checkOut);
        if (alreadyBooked >= room.getTotalRooms()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "This room is fully booked for the selected dates");
        }

        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /**
     * Package equivalent of {@link #validateStay}: the package must be
     * bookable, the departure date real, and the party within the package's
     * declared group size.
     */
    private void validatePackageBooking(CreatePackageBookingRequest request, TourPackage tourPackage) {
        if (tourPackage.getStatus() != ContentStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "This package is not currently available to book");
        }

        int partySize = request.numberOfAdults() + request.numberOfChildren();

        if (partySize < tourPackage.getMinGroupSize()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This package requires at least " + tourPackage.getMinGroupSize() + " traveller(s)");
        }
        if (tourPackage.getMaxGroupSize() != null && partySize > tourPackage.getMaxGroupSize()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "This package takes at most " + tourPackage.getMaxGroupSize() + " traveller(s)");
        }

        // The traveller list is what actually gets recorded per person, so it
        // has to agree with the counts the price was calculated from.
        if (request.travellers().size() != partySize) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Please provide details for all " + partySize + " traveller(s)");
        }
    }

    /**
     * Guests don't have accounts, but bookings need a {@code users} row. Repeat
     * bookings from the same address reuse it, so a guest's history stays
     * joined up if customer login is added later. Existing rows keep their
     * stored name — a booking form typo shouldn't silently rewrite an account.
     */
    private User findOrCreateGuest(GuestDetailsRequest guest) {
        String email = normalizeEmail(guest.email());

        Optional<User> existing = userRepository.findByEmailAndDeletedAtIsNull(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getPhone() == null) {
                user.setPhone(guest.phone());
                user.setUpdatedAt(Instant.now());
                userRepository.save(user);
            }
            return user;
        }

        Instant now = Instant.now();
        return userRepository.save(User.builder()
                .firstName(guest.firstName().trim())
                .lastName(guest.lastName().trim())
                .email(email)
                .phone(guest.phone())
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void saveTravellers(UUID bookingId, List<TravellerRequest> travellers, Instant now) {
        // uq_booking_travellers_one_lead allows at most one lead per booking;
        // if the client didn't nominate one, the first traveller becomes it.
        boolean anyLead = travellers.stream().anyMatch(TravellerRequest::isLeadTraveller);

        List<BookingTraveller> rows = new java.util.ArrayList<>();
        for (int i = 0; i < travellers.size(); i++) {
            TravellerRequest t = travellers.get(i);
            boolean isLead = anyLead ? t.isLeadTraveller() : i == 0;

            rows.add(BookingTraveller.builder()
                    .bookingId(bookingId)
                    .fullName(t.fullName().trim())
                    .dateOfBirth(t.dateOfBirth())
                    .gender(t.gender())
                    .passportNumber(t.passportNumber())
                    .passportExpiry(t.passportExpiry())
                    .nationalityCountryId(t.nationalityCountryId())
                    .leadTraveller(isLead)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        // Guard the unique index rather than letting the insert fail: if the
        // client marked two leads, that's a request error, not a 500.
        if (rows.stream().filter(BookingTraveller::isLeadTraveller).count() > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only one traveller can be the lead traveller");
        }

        bookingTravellerRepository.saveAll(rows);
    }

    /**
     * Payment is a placeholder: no gateway is integrated, so this records the
     * intended method and leaves the row PENDING with no {@code paid_at}. When
     * a real provider is wired in, this is where its transaction id and
     * SUCCESS/FAILED result land — the schema already has the columns.
     */
    private void recordPlaceholderPayment(Booking booking, PaymentMethod paymentMethod, Instant now) {
        bookingPaymentRepository.save(BookingPayment.builder()
                .bookingId(booking.getId())
                .amount(booking.getTotalAmount())
                .currencyCode(booking.getCurrencyCode())
                .paymentMethod(paymentMethod)
                .provider("PLACEHOLDER")
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private BookingResponse buildResponse(Booking booking) {
        User guest = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + booking.getUserId()));

        List<BookingTravellerResponse> travellers = bookingTravellerRepository
                .findByBookingIdOrderByLeadTravellerDescFullNameAsc(booking.getId()).stream()
                .map(t -> new BookingTravellerResponse(
                        t.getId(), t.getFullName(), t.getDateOfBirth(), t.getGender(),
                        t.getPassportNumber(), t.getPassportExpiry(), t.isLeadTraveller()))
                .toList();

        BookingPaymentResponse payment = bookingPaymentRepository
                .findFirstByBookingIdOrderByCreatedAtDesc(booking.getId())
                .map(p -> new BookingPaymentResponse(
                        p.getId(), p.getAmount(), p.getCurrencyCode(),
                        p.getPaymentMethod(), p.getStatus(), p.getPaidAt()))
                .orElse(null);

        return booking.getBookingType() == BookingType.PACKAGE
                ? buildPackageResponse(booking, guest, travellers, payment)
                : buildHotelResponse(booking, guest, travellers, payment);
    }

    private BookingResponse buildHotelResponse(
            Booking booking, User guest,
            List<BookingTravellerResponse> travellers, BookingPaymentResponse payment) {
        HotelRoom room = hotelRoomRepository.findById(booking.getHotelRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + booking.getHotelRoomId()));
        Hotel hotel = hotelRepository.findById(room.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + room.getHotelId()));
        City city = cityRepository.findById(hotel.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + hotel.getCityId()));
        Country country = countryRepository.findById(city.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + city.getCountryId()));
        String roomTypeName = roomTypeRepository.findById(room.getRoomTypeId())
                .map(RoomType::getName)
                .orElse(null);

        int nights = (int) ChronoUnit.DAYS.between(booking.getTravelDate(), booking.getReturnDate());

        return new BookingResponse(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getBookingType(),
                booking.getStatus(),
                hotel.getName(),
                hotel.getSlug(),
                room.getName(),
                roomTypeName,
                room.getPricePerNight(),
                nights,
                null, null, null, null, null, null,
                city.getName(),
                country.getName(),
                booking.getTravelDate(),
                booking.getReturnDate(),
                booking.getNumberOfAdults(),
                booking.getNumberOfChildren(),
                booking.getTotalAmount(),
                booking.getCurrencyCode(),
                guest.fullName(),
                guest.getEmail(),
                guest.getPhone(),
                booking.getSpecialRequests(),
                booking.getCancelledAt(),
                booking.getCancellationReason(),
                booking.getCreatedAt(),
                travellers,
                payment);
    }

    private BookingResponse buildPackageResponse(
            Booking booking, User guest,
            List<BookingTravellerResponse> travellers, BookingPaymentResponse payment) {
        TourPackage tourPackage = tourPackageRepository.findById(booking.getPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Package not found: " + booking.getPackageId()));
        City city = cityRepository.findById(tourPackage.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found: " + tourPackage.getCityId()));
        Country country = countryRepository.findById(tourPackage.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + tourPackage.getCountryId()));

        return new BookingResponse(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getBookingType(),
                booking.getStatus(),
                null, null, null, null, null, null,
                tourPackage.getTitle(),
                tourPackage.getSlug(),
                tourPackage.getDurationDays(),
                tourPackage.getDurationNights(),
                packagePricing.pricePerAdult(tourPackage),
                packagePricing.pricePerChild(tourPackage),
                city.getName(),
                country.getName(),
                booking.getTravelDate(),
                booking.getReturnDate(),
                booking.getNumberOfAdults(),
                booking.getNumberOfChildren(),
                booking.getTotalAmount(),
                booking.getCurrencyCode(),
                guest.fullName(),
                guest.getEmail(),
                guest.getPhone(),
                booking.getSpecialRequests(),
                booking.getCancelledAt(),
                booking.getCancellationReason(),
                booking.getCreatedAt(),
                travellers,
                payment);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private Booking findBookingOrThrow(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

}
