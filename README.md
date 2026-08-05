# TourPackage — API

Backend for the TourPackage travel agency platform. The database schema is fully designed and
migrated (see [`docs/DATABASE.md`](./docs/DATABASE.md)). Admin authentication is fully implemented
end-to-end (see [Authentication](#authentication) below), a read-only **public content API**
powers the marketing homepage (see [Public Content API](#public-content-api)), the **Hotel
Module** (see [Hotel Module](#hotel-module)) adds full CRUD for hotels/images/rooms/amenities/room
types, and the **Tour Package Module** (see [Tour Package Module](#tour-package-module)) adds full
CRUD for packages/images/itinerary/includes/excludes — both with public search, filter, sort, and
pagination. **Booking** (see [Booking Module](#booking-module)) is implemented end-to-end for both
hotels and tour packages: guest checkout, availability, per-person pricing, booking numbers,
confirmation emails, booking history, and admin status management. **Enquiries** from the contact
page (see [Contact & Enquiries](#contact--enquiries)) are saved, acknowledged, and worked from an
admin inbox. The **Admin Dashboard** API (see [Admin Dashboard](#admin-dashboard)) adds reporting
statistics plus back-office endpoints for customers, newsletter, testimonials, FAQs, and settings.
A **CMS** (see [CMS](#cms)) puts the site's editorial layer — slider, banners, gallery, blog,
section copy, navigation, and SEO — behind admin-editable APIs.

## Tech Stack

- Spring Boot 3 (3.3.5) + Java 21
- PostgreSQL + Spring Data JPA
- Flyway (database migrations)
- Spring Security + JWT authentication ([jjwt](https://github.com/jwtk/jjwt))
- Lombok
- MapStruct
- Spring Mail

## Package Structure

```
com.tourpackage.api
├── controller/   # REST controllers — AuthController implemented; rest empty
├── service/      # Business/application services — auth services implemented
├── repository/   # Spring Data JPA repositories — auth repositories implemented
├── entity/       # JPA entities — Admin + auth token entities implemented
├── dto/
│   ├── request/  # Validated request bodies
│   └── response/ # ApiResponse<T> envelope + response DTOs
├── mapper/       # MapStruct entity <-> DTO mappers
├── config/       # Cross-cutting configuration (CORS, async, ...)
├── security/     # Spring Security + JWT (SecurityConfig, JwtService, filters, UserDetails)
└── exception/    # Global exception handling
```

`controller`, `service`, `repository`, `entity`, and `mapper` now contain the admin-auth slice plus
a public read-only content slice (`Country`, `City`, `Hotel`, `HotelImage`, `TourPackage`,
`PackageImage`, `Testimonial`, `Faq`, `Banner`, `BlogPost`, `Setting`, `NewsletterSubscriber`).
Booking/payment entities and any admin-side content management (create/update hotels, packages,
etc.) still hold only `package-info.java` placeholders until that feature work begins.

## Authentication

Admin-only (back-office) authentication — there is no customer-facing (`users` table) auth yet.

### Endpoints

All under `/api/auth`. Every response is wrapped in the standard envelope
(`{ success, message, data, timestamp }` on success; `{ timestamp, status, error, message, path,
fieldErrors }` on failure, via `GlobalExceptionHandler`).

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/login` | Public | Email/password → access + refresh token. Locks after 5 failed attempts (15 min); blocks unverified/disabled accounts. |
| POST | `/refresh` | Public (bearer refresh token in body) | Rotates the refresh token (old one is single-use). Reusing an already-rotated token revokes **every** active session for that admin — reuse implies theft. |
| POST | `/forgot-password` | Public | Always returns the same generic message regardless of whether the email exists (no account enumeration). |
| POST | `/reset-password` | Public (token in body) | Single-use token; on success, revokes all existing refresh tokens (forces re-login everywhere). |
| GET | `/verify-email` | Public (token in query) | Consumes an email-verification token. Not in the original spec's endpoint list, but added because "Email Verification" was a required feature with no other way to complete it — see below. |
| GET | `/profile` | Bearer JWT | Current admin's profile. |
| PUT | `/profile` | Bearer JWT | Updates `fullName`/`phone`/`avatarUrl` only — not email or role. |

**Email verification, without a registration endpoint:** nothing in this task's scope creates new
admins via the API, so there's no natural "just signed up" moment to send a verification email
from. Instead, `login()` sends one the first time an unverified admin authenticates successfully
(correct password, but `email_verified_at IS NULL`) and skips re-sending while a still-valid token
is already pending, so repeated attempts don't flood the inbox.

### Bootstrap admin

`V11__seed_bootstrap_admin.sql` seeds one pre-verified `SUPER_ADMIN` so there's an account to log
in with on a fresh database:

```
email:    admin@tourpackage.com
password: ChangeMe123!
```

This password is public (committed to source control) — change it immediately in any real
environment.

### Design notes

- **Refresh tokens are opaque random strings, not JWTs**, stored server-side as a SHA-256 hash
  (`refresh_tokens` table) — never the raw value. Unlike a stateless JWT, they can be revoked
  before their natural expiry, which is what makes logout, password-reset invalidation, and reuse
  detection possible at all. Password-reset and email-verification tokens follow the same
  hash-only-at-rest pattern.
- **Role-based access is fully wired**: the access token carries the admin's role as a claim,
  `JwtAuthenticationFilter` turns it into a `ROLE_<role>` authority on the `SecurityContext`, and
  `@EnableMethodSecurity` is on, so `@PreAuthorize("hasRole('SUPER_ADMIN')")` works on any endpoint
  today. None of the 7 auth endpoints above are role-gated (they're all self-service), so there's
  no example of it in use yet — it'll apply naturally once admin-management endpoints exist.
- **A transaction-rollback trap worth knowing about**: several flows here intentionally throw
  right after writing something that must survive regardless (e.g. `login()` records a failed
  attempt, then throws `InvalidCredentialsException`; refresh-token reuse detection revokes every
  session, then throws). Because the throwing method is `@Transactional`, Spring's
  default rollback-on-`RuntimeException` would otherwise silently undo that write. Both
  `LoginSecurityService` and `TokenRevocationService` exist specifically to run those writes in
  `Propagation.REQUIRES_NEW` — a separate transaction that commits independently of the caller.
  This was caught by testing the actual behavior (reuse a rotated refresh token twice in a row),
  not by inspection — if you add a similar "mutate, then throw" flow, use the same pattern.
- **Email columns are `VARCHAR`, not `citext`**, despite `docs/DATABASE.md`'s original rationale
  for citext. Hibernate's PostgreSQL dialect doesn't recognize `citext` under `ddl-auto=validate`
  and fails schema validation at boot. Case-insensitivity is instead handled by lowercasing at the
  application layer (`AuthService.normalize`) before every read/write.

## Public Content API

Read-only endpoints (plus one write — newsletter signup) that power the marketing homepage. No
auth required; all under `/api/public`.

| Method | Path | Notes |
|---|---|---|
| GET | `/banners` | Active hero-slider banners, ordered, filtered to their active date range. |
| GET | `/destinations/popular?limit=8` | Cities flagged `is_popular`, with a live count of published tour packages per city. |
| GET | `/hotels/top?limit=8` | Published hotels ordered by rating, with city/country name and cover image joined in. |
| GET | `/tour-packages/best?limit=8` | Published packages ordered by featured flag then rating. |
| GET | `/tour-packages/offers?limit=6` | Published packages with a `discount_price` set, ordered by discount percentage. |
| GET | `/testimonials/featured?limit=6` | Featured testimonials, with customer country and package title joined in. |
| GET | `/blog-posts/recent?limit=6` | Published posts ordered by `published_at`, with author name joined in. |
| GET | `/faqs` | All active FAQs, ordered by category then display order. |
| GET | `/settings` | Flattened `{key: value}` map of only the settings flagged `is_public` — drives the footer's contact info and social links. Anything not flagged public (SMTP creds, payment keys) never reaches this endpoint. |
| POST | `/newsletter/subscribe` | `{ email }` → idempotent subscribe; resubscribing a previously-unsubscribed address reactivates it instead of erroring. |

### Design notes

- **Card-level DTOs come straight from JPQL constructor-expression projections** (`HotelRepository`,
  `TourPackageRepository`, etc.), not entity-to-DTO mapping — a homepage card needs a handful of
  joined fields (city name, cover image, price), and fetching full entity graphs just to discard
  most of it would be wasteful. `DestinationResponse`'s package count is the one exception: it's
  computed as a separate grouped query and merged in `DestinationService`, since a per-city COUNT
  subquery in the same projection would've been awkward to express cleanly.
- **Demo content is seeded via Flyway** (`V13__seed_demo_content.sql`) — 5 countries, 8 cities, 8
  hotels, 8 tour packages (4 with discounts), 6 testimonials, 8 FAQs, 4 banners, 6 blog posts, and
  the public settings the footer reads. This is what lets the homepage render real content instead
  of empty states out of the box; replace it via the (future) admin content screens, not by hand-
  editing this migration.
- **Two more Hibernate/Postgres type mismatches were caught by actually booting the app**, not by
  code review: `CHAR(n)` columns (`iso2`, `iso3`, `currency_code`) map to Hibernate's default
  `VARCHAR` for a `String` field, so `ddl-auto=validate` rejected them the same way it rejected
  `citext` earlier — fixed by converting those columns to `VARCHAR(n)` in `V3`/`V5`/`V6`/`V7`
  (nothing was relying on `CHAR`'s blank-padding behavior). See `docs/DATABASE.md` for the full
  writeup.

## Hotel Module

Full CRUD for hotels plus their nested images/rooms/amenities, and standalone master-data CRUD for
amenities and room types. Admin endpoints are role-gated; public endpoints power the frontend's
`/hotels` listing and `/hotels/[slug]` detail pages.

### Admin endpoints

All under `/api/admin/**`, requiring a bearer JWT. Mutating endpoints additionally require
`SUPER_ADMIN`, `ADMIN`, or `EDITOR` (`@PreAuthorize("hasAnyRole(...)")`); GET endpoints just require
being logged in.

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/hotels` | Paginated, filterable by `status`/`cityId`/`search`. Row-level DTO (`HotelAdminListResponse`) — no nested images/amenities/rooms, to avoid N+1 on the list page. |
| GET/POST/PUT/DELETE | `/admin/hotels[/{id}]` | Full hotel CRUD. Delete is a soft delete (`deleted_at`), which also frees the slug for reuse — the unique-slug check is scoped to `deleted_at IS NULL`. |
| POST/PUT/DELETE | `/admin/hotels/{id}/images[/{imageId}]` | Nested image CRUD, scoped by `hotelId` so one hotel's admin can't touch another's image by guessing a UUID. Setting `isCover: true` clears any other cover image for that hotel first (DB has a partial unique index allowing only one). |
| POST/PUT/DELETE | `/admin/hotels/{id}/rooms[/{roomId}]` | Nested room CRUD, same hotelId-scoping. "Availability" isn't a separate concept — it's derived (`isActive && totalRooms > 0`). |
| GET/POST/PUT/DELETE | `/admin/amenities[/{id}]` | Master data. Delete has no guard (removing an amenity just drops it from any hotel's `hotel_amenities` rows). |
| GET/POST/PUT/DELETE | `/admin/room-types[/{id}]` | Master data. Delete is guarded — rejected with 409 if any `hotel_rooms` row still references it. |

Amenities on a hotel are a plain many-to-many set, assigned via `HotelRequest.amenityIds` on
create/update using replace-all (delete all existing `hotel_amenities` rows for that hotel, then
re-insert). Images and rooms are **not** part of that request — they get their own nested endpoints
above, added after the hotel itself exists.

### Public endpoints

All under `/api/public/**`, no auth required.

| Method | Path | Notes |
|---|---|---|
| GET | `/public/hotels` | Paginated + filterable (`cityId`, `countryId`, `minPrice`, `maxPrice`, `minStarRating`, `search`, `amenityIds`) + sortable (standard Spring `sort=property,direction`, e.g. `sort=basePrice,asc`). |
| GET | `/public/hotels/{slug}` | Full detail — published + not-deleted only. 404 otherwise. |
| GET | `/public/hotels/top?limit=8` | Unchanged from the homepage module. |
| GET | `/public/amenities` | Active amenities only, for the listing page's filter checkboxes. |

### Design notes

- **A Hibernate/PostgreSQL null-parameter type-inference bug, caught only by booting against real
  Postgres**: the `search` filter's JPQL was `LOWER(h.name) LIKE LOWER(CONCAT('%', :search, '%'))`
  guarded by `:search IS NULL OR ...`. When `:search` is bound `null` (the common case — no search
  term), PostgreSQL's extended query protocol has to resolve a type for the parameter before it can
  even short-circuit the `OR`, and it inferred `bytea` instead of `text` — every unfiltered list
  call 500'd with `function lower(bytea) does not exist`. Fixed by explicitly casting:
  `LOWER(CONCAT('%', CAST(:search AS string), '%'))`. `mvn compile` and unit-level reasoning had
  nothing to say about this; only an actual request against a real database surfaced it.
- **Amenity-list filtering avoids a second collection-parameter null ambiguity by construction**:
  rather than trying to make `:amenityIds IS NULL OR ... IN :amenityIds` work (Hibernate handles
  `IN` on a null/empty collection inconsistently across versions), the service always passes a
  non-null (possibly empty) list plus a separate scalar `amenityCount`, and the query is
  `:amenityCount = 0 OR EXISTS (SELECT 1 FROM HotelAmenity ha WHERE ha.hotelId = h.id AND
  ha.amenityId IN :amenityIds)`. Confirmed against real data, not just reasoned through.
- **Nested resources are looked up by `(id, hotelId)`, never `id` alone** (`findByIdAndHotelId` on
  both `HotelImageRepository` and `HotelRoomRepository`) — an IDOR guard so an admin with access to
  one hotel can't mutate another hotel's image/room by supplying its UUID in the URL of an endpoint
  scoped to a different `hotelId`. Verified with a live request: updating a real room's ID through
  the wrong hotel's URL 404s instead of succeeding.
- **`HotelAdminResponse`/`HotelPublicDetailResponse` are assembled by hand in the service, not via
  MapStruct** — both need city/country names (joined from separate tables) and nested
  images/amenities/rooms lists built from three separate repository calls, which doesn't fit a
  single entity-to-DTO mapping. The public detail response reuses the same
  `buildImages`/`buildAmenities`/`buildRooms` helpers as the admin response (package-private on
  `HotelAdminService`, called from `HotelService`) rather than duplicating that assembly logic.
- **Demo amenities/room-types/rooms are seeded via `V14__seed_hotel_amenities_rooms.sql`** — the
  original homepage seed (`V13`) only had hotels and their cover images, so the new filters/gallery/
  rooms UI would otherwise render empty states for every seeded hotel. 12 amenities, 3 room types,
  and a proportionally-priced room per type for each of the 8 seeded hotels.

## Tour Package Module

Full CRUD for tour packages plus their nested images, day-by-day itinerary, and
included/excluded line items. Same shape as the Hotel Module: role-gated admin endpoints,
public endpoints backing the frontend's `/packages` and `/packages/[slug]` pages.

### Admin endpoints

All under `/api/admin/tour-packages`, requiring a bearer JWT; mutations additionally require
`SUPER_ADMIN`, `ADMIN`, or `EDITOR`.

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/tour-packages` | Paginated, filterable by `status`/`cityId`/`search`. Row-level DTO with no nested collections. |
| GET/POST/PUT/DELETE | `/admin/tour-packages[/{id}]` | Full CRUD. Delete is a soft delete (`deleted_at`), which frees the slug for reuse. |
| POST/PUT/DELETE | `/admin/tour-packages/{id}/images[/{imageId}]` | Nested image CRUD, scoped by `packageId`. `isCover: true` clears any existing cover first (partial unique index allows one). |
| POST/PUT/DELETE | `/admin/tour-packages/{id}/itinerary[/{dayId}]` | Day-by-day itinerary. `day_number` is unique per package — a collision returns 409 rather than surfacing the DB constraint as a 500. |
| POST/DELETE | `/admin/tour-packages/{id}/includes[/{includeId}]` | "What's included" line items. |
| POST/DELETE | `/admin/tour-packages/{id}/excludes[/{excludeId}]` | "What's excluded" line items. |

### Public endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/public/tour-packages` | Paginated + filterable (`cityId`, `countryId`, `minPrice`, `maxPrice`, `minDurationDays`, `maxDurationDays`, `difficultyLevel`, `discountedOnly`, `search`) + sortable via `sort=property,direction`. |
| GET | `/public/tour-packages/{slug}` | Full detail — published + not-deleted only, 404 otherwise. |
| GET | `/public/tour-packages/best`, `/offers` | Unchanged from the homepage module. |
| GET | `/public/destinations` | All active cities, for the listing page's destination filter dropdown (the existing `/popular` variant is capped and filtered to `is_popular`, so it can't back a full filter list). |

### Design notes

- **Includes and excludes share one DTO pair.** `package_includes` and `package_excludes` are
  column-for-column identical, and which list a row belongs to is decided entirely by the endpoint
  it was posted to — so `PackageLineItemRequest`/`PackageLineItemResponse` serve both rather than
  duplicating two identical records. `PackageLineItemMapper` overloads `toResponse` on the two
  entity types.
- **Price filters compare against the effective price, not the list price**:
  `COALESCE(tp.discountPrice, tp.price)`. A package listed at $1,399 but selling for $1,099 should
  appear under a "max $1,200" filter — filtering on `price` alone would hide the very packages a
  price-sensitive user is looking for.
- **`difficulty_level` is a JPA enum, not the `String` it was before.** The column always had a
  `CHECK (difficulty_level IN (...))` constraint, so the set of legal values was already closed;
  mapping it as `@Enumerated(EnumType.STRING) DifficultyLevel` moves that from a runtime DB error
  to a compile-time type, and lets the controller bind `?difficultyLevel=MODERATE` directly.
- **Two DB constraints are pre-checked in the service** (`discount_price <= price`, and
  `max_group_size >= min_group_size`). Both are enforced by the database regardless, but a raw
  constraint violation surfaces as an opaque 500 — checking first turns them into a 400 that names
  the actual problem.
- **The `CAST(:search AS string)` workaround was applied up front here**, carried over from the
  Hotel Module's `lower(bytea)` failure rather than rediscovered. See that section for the full
  explanation of why a null-bound parameter needs an explicit cast.
- **Seed data spans three migrations**: `V15` adds a full itinerary (one row per day, generated
  from each package's `duration_days`) plus include/exclude line items for all 8 demo packages;
  `V16` varies `difficulty_level`, which V13 had left at the column default so that three of the
  four difficulty-filter options returned zero results. `EXTREME` is deliberately left empty —
  none of the demo packages are expedition-grade, and inventing one to fill out the enum would
  misrepresent the content.

## Booking Module

Booking for both hotel rooms and tour packages, end to end: a guest picks what they want, supplies
their own and their travellers' details, and gets a booking number back. The booking lands as
`PENDING` and an admin moves it through `CONFIRMED` → `COMPLETED`, or `CANCELLED`.

### Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/public/bookings/hotel` | None | Room + check-in/check-out. Priced per night. |
| POST | `/public/bookings/package` | None | Package + departure date. Priced per person; the return date is derived from the package's duration. |
| GET | `/public/bookings/{bookingNumber}?email=` | None | Retrieves one booking. Requires the email it was made with. |
| GET | `/public/bookings/history?bookingNumber=&email=` | None | Every booking made with that email, once one reference has been verified — see below. |
| GET | `/admin/bookings` | Bearer JWT | Paginated, filterable by `status`, searchable by booking number / guest name / email. Newest first. |
| GET | `/admin/bookings/{id}` | Bearer JWT | Full detail incl. travellers and payment. |
| PATCH | `/admin/bookings/{id}/status` | `SUPER_ADMIN`, `ADMIN`, `EDITOR`, **`SUPPORT`** | Confirm / cancel / complete. |

### Hotel vs. package

Both types share the `bookings` table (`booking_type` discriminates, and a CHECK constraint
enforces that exactly one of `package_id`/`hotel_room_id` is populated), and both come back as one
`BookingResponse` shape with the other type's fields null. One shape rather than two because
booking history returns them interleaved, and a client rendering a mixed list shouldn't have to
switch on the response type before it can read the booking number. `travel_date`/`return_date` are
shared columns surfaced as `startDate`/`endDate`; the label differs by type (check-in/check-out vs.
departure/return) and belongs to the UI.

| | Hotel | Package |
|---|---|---|
| Dates | Guest picks both | Guest picks departure; return is derived from `duration_days` |
| Pricing | `price_per_night × nights` | `pricePerAdult × adults + pricePerChild × children` |
| Capacity | Room's `max_adults`/`max_children`, plus a real overlap check against inventory | Package's `min_group_size`/`max_group_size` |

### Package pricing

`PackagePricing` is the single place package rates are decided — both the public package detail
response (which exposes `pricePerAdult`/`pricePerChild` so the booking form can total a party) and
the booking service (which charges them) go through it, so a quoted price and a charged price
can't drift apart. The adult rate is the package's `discount_price` when it's on offer, otherwise
list price; children pay a configurable percentage of it (`CHILD_PRICE_PERCENT`, default 70).

That percentage is a single config value rather than a `tour_packages` column because it's
currently a uniform business rule, not per-package data. If packages ever need their own child
policies, it becomes a column and `PackagePricing` reads it off the entity instead — the rest of
the code doesn't change.

### Booking history without accounts

Listing bookings by email alone would let anyone enumerate someone else's trips. Instead the caller
has to prove they hold one valid reference for that address first, and then gets everything booked
with it — the same "prove one, see all" shape a *manage my booking* page uses when there's no login
to lean on. The ownership check reuses the single-booking lookup, so it and its deliberately-generic
404 live in exactly one place.

`SUPPORT` can change booking status but cannot touch hotel or package content — working a booking
queue is precisely what that role exists for, whereas editing the catalogue isn't.

### Design notes

- **Booking numbers come from the database, not the application.** The column already had a
  `DEFAULT 'TP-' || year || lpad(nextval('booking_reference_seq'), 6, '0')`, which is atomic under
  concurrent inserts in a way that "read the highest number and add one" is not. The entity maps it
  `insertable = false` with Hibernate's `@Generated(event = INSERT)` so Hibernate doesn't overwrite
  the default with a null and reads the generated value back after the INSERT.
- **Guests have no accounts, but `bookings.user_id` is `NOT NULL`.** Rather than loosen the schema,
  the booking flow's guest-details step creates a `users` row with a null `password_hash` — which
  the column already allowed, since it was written that way for social-login accounts. Repeat
  bookings from the same address (compared lowercase) reuse that row, so history stays joined up if
  customer login is added later. An existing row keeps its stored name: a typo in one booking form
  shouldn't silently rename an account.
- **Availability is a real check against overlapping bookings**, not just the room's `is_active`
  flag: `COUNT(*)` of `PENDING`/`CONFIRMED` bookings for that room where
  `travel_date < :checkOut AND return_date > :checkIn`, compared against `total_rooms`. The
  comparison is strict on both sides so the checkout day is free — back-to-back stays are allowed,
  which is the behaviour a hotel actually wants. `CANCELLED` bookings release their inventory;
  `PENDING` ones hold it, since an unpaid booking that hasn't been rejected is still a claim.
- **The whole flow is one request, not a server-side wizard.** The frontend collects it over four
  steps, but nothing is persisted until the guest confirms — so an abandoned booking simply doesn't
  exist, rather than sitting in the database as a row nobody will ever complete and that
  availability has to be taught to ignore.
- **Package bookings require one traveller row per person.** `numberOfAdults + numberOfChildren` is
  what the price was calculated from, and the traveller list is what actually gets recorded per
  person, so a mismatch between them is rejected rather than silently accepted — otherwise a party
  of four could be charged for four and manifest as one.
- **Payment is a placeholder.** No gateway is wired up, so `recordPlaceholderPayment` writes a
  `booking_payments` row with the intended method, `provider = 'PLACEHOLDER'`, status `PENDING`, and
  no `paid_at`. The schema already has the transaction-id and raw-response columns a real provider
  would fill in; `raw_response` (JSONB) is deliberately left unmapped in the entity until something
  writes it.
- **Emails are `@Async` and swallow their own failures.** Both the customer confirmation and the
  admin notification go out after the booking is committed, and `MailService.send` logs rather than
  throws — a mail outage must not roll back a booking the guest has already committed to. Status
  changes send a fourth template, worded per target status.
- **Status transitions are guarded in the service**, not just by the DB's
  `ck_bookings_cancellation` constraint: a cancelled or completed booking is terminal, `COMPLETED`
  is only reachable from `CONFIRMED`, and re-applying the current status is a 409. The constraint
  requires `cancelled_at` to be set if and only if the status is `CANCELLED`, so moving *out* of
  cancelled has to clear it — both directions are handled.
- **Guessable booking numbers are gated on the email.** The number is a visible sequence, so
  `GET /public/bookings/{number}` also requires the address the booking was made with, and returns
  the same 404 whether the number doesn't exist or the email doesn't match — confirming that a
  number is real but the email is wrong would leak which numbers exist.

## Contact & Enquiries

The contact page's form writes to `inquiries`, notifies the team, and acknowledges the sender.
Newsletter signup is unchanged — it's the same `POST /public/newsletter/subscribe` the homepage
footer has always used.

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/public/inquiries` | None | Saves the enquiry, emails the team, acknowledges the sender. |
| GET | `/admin/inquiries` | Bearer JWT | Paginated inbox, filterable by `status`, searchable by name/email. Newest first. |
| GET | `/admin/inquiries/{id}` | Bearer JWT | Full detail, with the linked package if there is one. |
| PATCH | `/admin/inquiries/{id}/status` | `SUPER_ADMIN`, `ADMIN`, `EDITOR`, **`SUPPORT`** | `NEW` → `IN_PROGRESS` → `RESOLVED` / `CLOSED`. |

### Design notes

- **One table, not two.** The schema has both `inquiries` and `contacts`, but `travel_date` and
  `party_size` are nullable, so the same form serves "planning a trip, here are the details" and
  "just have a question". Splitting near-identical submissions across two tables would mean two
  inboxes for staff to watch and two places to keep in sync; `contacts` stays unused rather than
  being filled for the sake of it.
- **`packageId` is context, not a requirement.** It's set when the visitor arrives from a package's
  "Ask About This Trip" link so the reply knows which trip they mean. An unknown or removed package
  id is *dropped* rather than rejected — failing a genuine enquiry because a stale link pointed at
  a deleted package would lose the enquiry and help nobody.
- **The message is escaped before it goes into the notification email**, not before it's stored.
  Escaping is an output concern: the database keeps what the visitor actually typed, and the HTML
  email gets an escaped copy so a message containing `<script>` renders as text rather than markup
  in whatever client the team reads it in.
- **`responded_at` records the first time an enquiry left the `NEW` pile** and isn't cleared by
  later status changes — it answers "how long did we take to reply", so overwriting it on a move
  from `IN_PROGRESS` to `RESOLVED` would destroy the only number worth having.

### A 500 that should always have been a 400

Sending an enum field a value outside its permitted set — `{"status": "BOGUS"}` — produced a **500**.
Jackson throws `HttpMessageNotReadableException` before the request ever reaches a controller, and
with no handler for it the global advice's catch-all turned a plain client mistake into what looked
like a server fault. This affected **every endpoint with an enum in its body**, so it had been
present since booking status was added; the contact work just happened to exercise it.

`GlobalExceptionHandler` now handles it as a 400 and, when Jackson identifies the offending field,
names it along with the accepted values:

```json
{ "status": 400, "message": "Validation failed",
  "fieldErrors": { "status": "Must be one of: NEW, IN_PROGRESS, RESOLVED, CLOSED" } }
```

Jackson's own message is deliberately not echoed back — it spells out the Java type and constant
names, which is internal shape a public API shouldn't leak.

## Admin Dashboard

Reporting statistics for the dashboard home, plus the back-office modules that didn't have admin
endpoints yet. Hotels, packages, bookings and enquiries already had theirs — see their own sections.

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/admin/dashboard/stats` | Bearer JWT | Revenue cards, 12-month chart series, status breakdown, top sellers, latest bookings — all in one response. |
| GET | `/admin/customers` | Bearer JWT | Paginated, searchable. Read-only. |
| GET/DELETE | `/admin/newsletter[/{id}]` | Bearer JWT / `SUPER_ADMIN`,`ADMIN`,`EDITOR` | List subscribers, filter by active. DELETE deactivates rather than deletes. |
| GET/POST/PUT/DELETE | `/admin/testimonials[/{id}]` | Bearer JWT / content roles | Full CRUD. |
| GET/POST/PUT/DELETE | `/admin/faqs[/{id}]` | Bearer JWT / content roles | Full CRUD. |
| GET/PUT | `/admin/settings` | Bearer JWT / **`SUPER_ADMIN`,`ADMIN` only** | Read all settings (including non-public ones the public API hides); update by key/value map. |

### Role model

Four roles, three permission tiers. The frontend mirrors these to hide actions, but the server is
the only thing enforcing them:

| | Read admin | Booking/enquiry status | Catalogue & content | Settings |
|---|---|---|---|---|
| `SUPER_ADMIN`, `ADMIN` | ✅ | ✅ | ✅ | ✅ |
| `EDITOR` | ✅ | ✅ | ✅ | ❌ |
| `SUPPORT` | ✅ | ✅ | ❌ | ❌ |

`SUPPORT` exists to work queues, so it can move bookings and enquiries through their statuses but
can't touch what's for sale. Settings are narrower still than content — they carry contact details,
mail configuration and payment keys — so only the two admin roles can write them.

### Design notes

- **One stats endpoint, not six.** The dashboard renders revenue, counts, two charts, top sellers
  and latest bookings together; splitting them would mean six round trips and six loading states
  for a screen that's only useful complete.
- **Revenue counts `CONFIRMED` and `COMPLETED` only.** A `PENDING` booking is a request nobody has
  agreed to yet — including it would inflate the headline figure with money that may never arrive.
- **Month buckets are grouped in SQL, and gaps are filled in Java.** The query only returns months
  that have rows; the service pads the rest to zero so the chart doesn't draw a straight line
  between two busy months and imply activity that didn't happen.
- **Percent change is null, not zero, when last month had no revenue.** There's no honest
  percentage increase from nothing, and the UI says so in words rather than rendering `∞%`.
- **Settings updates are all-or-nothing.** An unknown key rejects the whole request rather than
  applying the recognised half — a partial save on a settings screen leaves the admin with no way
  to tell what actually stuck. Unknown keys are rejected rather than created, since settings are
  defined by migrations and inventing rows would leave data nothing reads.
- **A third null-parameter type-inference failure.** `sumRevenue(from, to)` with both bounds null
  failed with `could not determine data type of parameter $1` — the same PostgreSQL behaviour
  behind the earlier `lower(bytea)` bug, in a different disguise. Fixed by splitting it into three
  methods that each take only the bounds they need, so no parameter is ever bound null. Casting
  would also have worked, but not needing the cast is better.

## CMS

Everything editorial on the public site is stored and served from here, so the frontend holds no
copy of its own. `V17__cms.sql` seeds each table with exactly the strings the components used to
hardcode — the site renders identically after the migration, but from the database.

### Public endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/public/cms/site-content` | Every active content block keyed by its dotted key, plus header and footer links — one call, because all of it is needed on every page. |
| GET | `/public/cms/seo?path=` | Metadata for one route. Returns `null` data when unmanaged, so the page falls back to its own defaults rather than failing. |
| GET | `/public/cms/gallery` | Active gallery photographs. |
| GET | `/public/cms/banners?placement=` | `HOME_SLIDER`, `OFFERS_STRIP`, or `PAGE_HERO`, filtered to the current scheduling window. |
| GET | `/public/cms/blog` | Paginated published posts, optionally by category. |
| GET | `/public/cms/blog/{slug}` | One published post. 404 for drafts. |

### Admin endpoints

All under `/admin/cms/**`; reads need a bearer JWT, writes need `SUPER_ADMIN`, `ADMIN`, or
`EDITOR`. Six resources, each with list / create / update / delete:
`blocks`, `seo`, `nav-links`, `gallery`, `banners`, `blog`.

### Data model

| Table | Holds |
|---|---|
| `content_blocks` | Section headings and page intros, addressed by a dotted key (`home.hotels`, `page.contact`) |
| `page_seo` | Per-route meta title, description, OG image, no-index flag |
| `nav_links` | Header and footer links, ordered, with a `nav_group` discriminator |
| `gallery_images` | Gallery photographs with categories |
| `banners` | Extended with `placement` so one table serves the slider, offers strip, and page heroes |
| `blog_posts` | Already existed; gained admin CRUD and a public detail endpoint |

### Design notes

- **A dotted key rather than a column per section.** `content_blocks` is addressed by
  `home.hotels`, `page.contact` and so on, so adding a section is a new row — not a migration, a
  new column, and a redeploy.
- **One `site-content` call, not one per section.** Navigation and headings are needed on every
  page render; splitting them across endpoints would mean several requests before anything paints.
- **`placement` instead of three banner tables.** A slider image, an offers banner and a page hero
  differ only in where they appear, so they share a table and a CHECK-constrained discriminator.
- **Missing SEO returns null, not 404.** A route without a managed row is a normal state — the page
  should fall back to its own defaults, and an error would make the whole page fail over metadata.
- **The last header link can't be deleted.** Removing every one would leave the site with no
  navigation and no way back to the screen that caused it, so the final one returns 409.
- **Publishing a post stamps `published_at`; unpublishing clears it.** `ck_blog_posts_published_at`
  requires the column set exactly when the status is `PUBLISHED`, so both directions are handled —
  and re-publishing keeps the original date rather than resetting it.

## Image Management

Uploads are processed in memory, written through a storage abstraction, and tracked in
`media_assets` so they can be listed and deleted. Existing tables keep storing plain URL strings,
which is why the seeded external image URLs still work alongside uploaded ones.

### Endpoints

| Method | Path | Notes |
|---|---|---|
| POST | `/admin/media?folder=` | Multipart, `files` repeated. Up to 20 per request. All-or-nothing. |
| GET | `/admin/media?folder=&page=&size=` | Paginated, newest first. |
| DELETE | `/admin/media/{id}` | Removes the row, then the file and its thumbnail. |
| PATCH | `/admin/cms/gallery/reorder` | `{ "ids": [...] }` — position in the list becomes `display_order`. |
| GET | `/uploads/**` | Public. Served by a resource handler with a one-year cache. |

Reads need a bearer JWT; writes need `SUPER_ADMIN`, `ADMIN`, or `EDITOR`.

### Processing

Every upload is decoded, scaled to fit `IMAGE_MAX_DIMENSION` on its longest side, re-encoded, and
given a `IMAGE_THUMBNAIL_DIMENSION` thumbnail. Images already smaller are not upscaled. Measured on
the test fixtures: a 3000x2000 PNG at 1.58 MB stores as a 2400x1600 JPEG at 930 KB; a 300x200 PNG at
143 KB stores at 14 KB.

### Adding a storage provider

Implement [`StorageService`](src/main/java/com/tourpackage/api/storage/StorageService.java) — three
methods — annotate it `@Service` and
`@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")`, then set
`STORAGE_PROVIDER=s3`. Nothing else changes: `MediaService` only ever sees the interface, and
`storage_provider` is recorded per row so assets written by one provider stay resolvable after a
switch. The interface deals in `byte[]` rather than streams because the image is already fully in
memory by the time it is stored, and every candidate SDK accepts bytes.

### Design notes

- **JDK ImageIO, no imaging dependency.** Resizing and JPEG quality are both reachable through
  `javax.imageio`; a library would have been a supply-chain addition for a few dozen lines.
- **Transparency is detected per pixel, not from the colour model.** `ColorModel.hasAlpha()` only
  says the buffer *has* an alpha channel, and anything drawn through an HTML canvas — everything the
  cropper returns — is RGBA even when fully opaque. Trusting it stored cropped photographs as PNG:
  the same 180x180 crop was 104 KB as PNG and 7.8 KB as JPEG.
- **Filenames are server-generated UUIDs.** The client's filename is kept for display only, so a
  hostile or merely awkward name can never influence a path.
- **Batch upload rolls back its own files.** The transaction rolls back the rows, but the filesystem
  has no such thing, so a failure part-way through deletes what it already wrote.
- **Delete removes the row first, then the files.** A leftover file is wasted disk; a row pointing at
  a deleted file is a broken image on the public site.
- **Storage keys are resolved against the root and rejected if they escape it**, so a crafted key
  cannot read or delete outside the upload directory.
- **The servlet multipart limits are separate from, and higher than, `IMAGE_MAX_UPLOAD_BYTES`.**
  Spring's 1 MB default would have rejected uploads before the application's own limit — and with a
  much less useful message.

## Email Module

Spring Mail over SMTP, with HTML templates on the classpath under
[`resources/email/`](src/main/resources/email/). Every message is sent
asynchronously — SMTP is a call to a third party, and a customer submitting a booking should not
wait on it, nor should a timeout there fail a booking that is already committed.

### What gets sent, and to whom

| Trigger | Admin | Customer |
|---|---|---|
| Hotel booked | `booking-notification` | `booking-confirmation` |
| Tour booked | `booking-notification` | `booking-confirmation` |
| Contact form submitted | `inquiry-notification` | `inquiry-acknowledgement` |
| Newsletter subscribed | `newsletter-notification` | — |
| Booking status changed | — | `booking-status-update` |
| Admin password reset | — | `password-reset` |
| Admin email verification | — | `email-verification` |

Admin mail goes to `MAIL_ADMIN_NOTIFICATIONS`, which is deliberately separate from `MAIL_FROM` so a
deployment can route it to a staffed inbox.

### Templates

Each file is the body of one email; [`layout.html`](src/main/resources/email/layout.html) wraps it
with the brand header and footer. Two placeholder forms, and the difference is the security story:
`{{value}}` is HTML-escaped, `{{{value}}}` is not. Escaping is the default so a value that reaches a
template without anyone thinking about it cannot inject markup — the raw form is only ever used for
fragments the service built itself.

Styles are inline and the layout is nested tables because email clients are not browsers: Outlook
renders through Word, Gmail strips `<style>` blocks on forwarded mail, and neither flexbox nor grid
can be relied on.

### Not Thymeleaf

`spring-boot-starter-thymeleaf` would auto-configure an MVC view resolver that a REST API has no use
for, to solve what is placeholder substitution over nine files.
[`EmailTemplateEngine`](src/main/java/com/tourpackage/api/service/EmailTemplateEngine.java) is ~150
lines instead. The trade-off is real: no loops or conditionals in templates, so anything repeating —
the booking details table — is built in Java and passed in as a raw fragment. A template needing a
loop would be the signal to reconsider.

### Design notes

- **Every message is `multipart/alternative`.** The text part is derived from the rendered HTML
  rather than hand-written, so there is one source of truth; a second copy of every template would
  drift from the first the moment anyone edited one. A missing text part shows raw markup in
  text-only clients and is a well-known spam-filter signal.
- **`Reply-To` is not `From`.** Several templates invite a reply, and `no-reply@` is not somewhere a
  reply can land.
- **Delivery failures are logged, never rethrown.** The booking or enquiry is already committed;
  losing it because an SMTP host was briefly unreachable would be far worse than a missing email.
- **`MAIL_ENABLED=false` logs instead of sending**, so a demo or a load test can run without mailing
  real people — and without deleting the SMTP configuration to do it.
- **The newsletter notification follows the subscription, not the request.** Subscribing is
  idempotent, so re-submitting the footer form sends nothing; a form that mailed the team on every
  submission would be trivial to turn into a flood. A reactivated address is distinguished from a
  new one because it reads differently to whoever gets the alert.
- **Dates are written out, statuses are lowercased.** `2026-11-14` is how the API stores a date;
  `Sat, 14 Nov 2026` is how someone reads their own check-in. And `CANCELLED` set into a sentence
  shouts at a customer whose trip just fell through.
- **`app.mail` binds as a record, unlike the rest of this codebase.** Nine String properties as
  constructor parameters are nine interchangeable positions where transposing two compiles cleanly
  and sends every customer email to the admin inbox.

## Global Search

One search box over hotels, packages, destinations (cities) and countries. There is no destinations
table — a destination *is* a city, with its country and package count — so the searchable types are
`HOTEL`, `PACKAGE`, `CITY` and `COUNTRY`.

### Endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/public/search/suggest?q=` | Autocomplete. Up to 5 hits per type, grouped. Returns empty groups below 2 characters rather than an error. |
| GET | `/public/search?q=&type=&page=&size=` | Paginated results across all types, or one. The only endpoint that counts towards popular searches. |
| GET | `/public/search/popular?limit=` | Most-used terms. |

### The indexes were already there

`hotels` and `tour_packages` have carried STORED weighted `search_vector` columns with GIN indexes
since V5 and V6, and `hotels.name`, `tour_packages.title` and `cities.name` have carried trigram
indexes just as long. **None of it was ever queried.** The existing search used
`LOWER(name) LIKE LOWER('%x%')`, and an index on `name` cannot serve a predicate on `lower(name)`.

Measured on 200k synthetic rows:

| Query shape | Plan | Buffers | Time |
|---|---|---|---|
| `LOWER(name) LIKE LOWER('%…%')` | Parallel Seq Scan | 2062 | 54.6 ms |
| `name ILIKE '%…%'` | Bitmap Index Scan | 528 | 6.1 ms |

So V19 adds only what was genuinely missing: a trigram index on `countries.name`, an index on
`cities.slug`, and the `search_queries` table.

### Typo tolerance

Matching is `name ILIKE :pattern OR :q <% name`. The second is **word** similarity (`<%`), not
whole-string similarity (`%`), because a short query is being compared against multi-word names: for
"serenty" against "Bali Serenity Villas", whole-string similarity is 0.261 and falls below the 0.3
default threshold, while word similarity is 0.625 and matches. The same trigram index serves both.

The threshold is lowered to 0.5 via `connection-init-sql` (`SEARCH_FUZZY_THRESHOLD`), because the
0.6 default rejects a single dropped letter in a short word — `word_similarity('tropcal', 'Bali
Tropical Retreat')` is 0.545. On the 200k-row corpus, 0.5 returned exactly the rows that genuinely
contain the word and no others, still as a bitmap index scan. It is set as runtime configuration
rather than in a migration: a migration would change the setting for every other connection to that
database too.

### Design notes

- **Native queries, not JPQL.** JPQL has no `ILIKE`, no trigram operators and no UNION, and all
  three are load-bearing. The cost is losing constructor-expression projections, so these use
  Spring Data interface projections instead.
- **`:type` is never null; callers pass `'ALL'`.** PostgreSQL cannot infer the type of a null bind
  parameter — a trap this codebase has hit more than once — and a sentinel avoids the question. It
  also lets the planner fold unselected branches to a one-time false filter rather than scanning them.
- **A UNION rather than four queries**, so one page of results can interleave types by relevance.
  Autocomplete does the opposite and queries per type, so that a type with many matches cannot crowd
  the others out of the dropdown.
- **Ranking is deliberately coarse** — exact title, then prefix, then anything else — with word
  similarity breaking ties inside each band. `title` is the final tiebreaker so equally-ranked rows
  keep a stable order between pages; without it, page 2 can repeat a row from page 1.
- **Only submitted searches are counted, and only ones that found something.** Counting autocomplete
  would fill the list with "b", "ba", "bal" — every prefix of every real search. And a term that
  returns nothing must never be promoted into a list of suggestions: a typo searched twice would
  otherwise be recommended to everyone.
- **One row per term, not one row per search.** The only question asked of `search_queries` is "what
  are the most common terms", and a log would grow without bound to answer it. Per-search history
  belongs in an analytics pipeline, not here.
- **`display_term` is capitalised for display only when it is entirely lowercase.** Most people type
  in lowercase, so the list otherwise drifts to "bali", "dubai", "paris" and reads like a bug —
  but title-casing everything would turn "USA" into "Usa".
- **LIKE metacharacters are escaped.** Not an injection guard, since the value is a bound parameter:
  it is so that searching for "50%" looks for two characters rather than "50 followed by anything".

## SEO Support

A single endpoint, because the frontend owns everything else about SEO.

| Method | Path | Notes |
|---|---|---|
| GET | `/public/seo/sitemap` | Slugs and `updatedAt` for every published hotel, package and blog post. |

The XML is generated by Next.js, which is the only side that knows the site's route structure — that
a hotel lives at `/hotels/{slug}` is a frontend fact. This endpoint supplies only what the database
knows: what is publicly visible, and when it last changed.

The projection is deliberately two fields. It is fetched for every entity on the site at once, and
reusing the listing DTOs would pull images and joins to build a file that contains neither.

`V20__seo_page_images.sql` backfills `page_seo.og_image_url`, which had been null on every row since
the CMS migration — so every static route, homepage included, shared as a bare link with no preview.
It also sets `no_index` on `/bookings`, which shows someone their own reservation.

## Production Readiness

### Observability

Every request gets an id, put in the SLF4J MDC and returned as `X-Request-Id`. It appears in the log
pattern, so the id from a bug report selects exactly the lines for that request out of an interleaved
log. An inbound id is honoured (so a trace started upstream survives) but length-capped and stripped
of anything but `[A-Za-z0-9._-]` first — it lands in both a log line and a response header, and
neither should be forgeable.

`AccessLogFilter` writes one line per request with status and duration; a 500 or anything slower than
`SLOW_REQUEST_MS` is logged at WARN. Query strings are deliberately **not** logged: they carry
password-reset and verification tokens.

Actuator exposes `health`, `info`, `metrics` and `prometheus`. Health is split into liveness
("restart this") and readiness ("stop routing to this", includes the database) — conflating them
restarts a pod that was only waiting on a slow dependency. Only `health` is public; metrics require
authentication, and a real deployment should bind actuator to an internal port.

A custom `storage` health indicator reports whether uploads can actually be written. Its failure is
otherwise silent: a container restarted without its volume is healthy by every other measure while
every upload fails.

### Rate limiting

Fixed window, three budgets: auth 20/min (password guessing), anonymous writes 10/min (form spam),
reads 300/min (scrapers). Runs **before** authentication, because the endpoints worth protecting are
the ones an unauthenticated caller can reach.

`X-Forwarded-For` is only trusted when `RATE_LIMIT_TRUST_PROXY=true`. The header is client-settable,
so trusting it without a proxy in front lets any caller bypass every limit — and exhaust the store's
key space while doing it.

### Redis-ready

Caching and rate limiting are both written against an interface with an in-memory default and a Redis
implementation selected by configuration. Nothing in the calling code changes. This matters because
the in-memory versions are **per JVM**: N instances behind a load balancer allow N times the
configured rate limit and hold N divergent caches. `CACHE_STORE=redis` and `RATE_LIMIT_STORE=redis`
make both shared.

Redis cache values are JSON, not JDK serialization — the cached types are DTOs that change shape
between releases, and a JDK-serialized cache fails to deserialize across a deploy rather than simply
missing.

### Caching

`@Cacheable` on the read paths every page hit touches: site content, page SEO, settings, destinations,
FAQs, popular searches. TTL is 60s by default and short on purpose — these sit in front of content an
admin edits and then immediately reloads the site to check. Admin writes evict the relevant caches, so
an edit is visible immediately rather than after the TTL.

### Startup safety check

Refuses to start under a `prod` profile with the development JWT secret, a wildcard or localhost CORS
origin, a localhost storage URL, or rate limiting disabled. It fails hard rather than warning because
the failure it prevents is silent: the application starts perfectly and signs tokens with a secret
that is in the repository. Outside production the same checks only warn — a developer should not have
to invent secrets to run the app.

### Exception handling

Auditing the handlers by probing every failure mode found **six cases returning 500 for what were
client errors**: wrong HTTP method, wrong content type, a non-numeric path parameter, a malformed
UUID, a missing required parameter, and an oversized upload. Each one also logged a stack trace, so
ordinary bad requests looked identical to real faults in the logs and error metrics. All six now
return the correct 4xx with a field-level message, and none echo the offending value back.

### Tests

63 tests. Unit tests cover pure logic — pricing rounding, template escaping, the plain-text
derivation, search normalisation and LIKE escaping, rate-limit windowing under 50 concurrent threads,
and the startup checks. Integration tests run the whole stack against a **real PostgreSQL**: the
schema uses `pg_trgm` indexes, generated `tsvector` columns and trigram operators, so a test passing
against an in-memory database would prove nothing. Flyway builds the test schema from the same
migrations that build production.

Rate limiting is off for the suite and re-enabled for the one class that tests it — shared counters
otherwise make unrelated tests fail each other.

### API documentation

springdoc at `/swagger-ui.html`, split into `public` and `admin` groups. One flat list of ~90
endpoints hides the single most important distinction: which need a token. `SWAGGER_ENABLED=false`
turns both the UI and the JSON off for production.

### CI

`.github/workflows/ci.yml` compiles, tests against a PostgreSQL service container, uploads reports
(on failure too — that is when they are wanted), packages the jar, and builds the Docker image
without pushing it.

### Docker

Runs as a non-root user with the upload directory created and owned in the image. `MaxRAMPercentage`
rather than a fixed heap, because the JVM otherwise sizes its heap from the host's memory rather than
the container limit and gets OOM-killed on a large box. `ExitOnOutOfMemoryError` turns an exhausted
heap into a restart rather than a process that stays up serving errors. `HEALTHCHECK` hits the
readiness probe.

## Getting Started

### Prerequisites

- Java 21
- A running PostgreSQL instance (or use the root [`docker-compose.yml`](../docker-compose.yml))
- An SMTP server for password-reset/verification emails in real use (Mailhog via docker-compose
  works for local dev); without one, mail sending fails silently and is logged — the rest of the
  flow (token creation, validation) still works.

The Maven Wrapper (`./mvnw`) is included, so a local Maven install is not required.

### Run locally

```bash
cp .env.example .env   # then export the vars, or set them in your run configuration
./mvnw spring-boot:run
```

The API runs at http://localhost:8080/api. Health check: http://localhost:8080/api/actuator/health.

### Build

```bash
./mvnw clean package
java -jar target/tourpackage-api.jar
```

### Database

Flyway migrations live in `src/main/resources/db/migration` (`V1`–`V16`): `V1`–`V9` are the core
domain schema (24 entities across 25 tables), `V10` adds the auth-token tables and
`admins.email_verified_at`, `V11` seeds the bootstrap admin, `V12` adds `blog_posts` and
`cities.image_url`, `V13` seeds demo homepage content, `V14` seeds demo amenities/room-types/rooms
for the Hotel Module, `V15`–`V16` seed itinerary/include/exclude rows and difficulty levels for the
Tour Package Module, and `V17` adds the CMS tables (content blocks, page SEO, nav links, gallery)
plus `banners.placement`, seeded with the copy the frontend previously hardcoded. Full ER diagram,
relationship rationale, indexing strategy, and constraint reference:
**[docs/DATABASE.md](./docs/DATABASE.md)**.

`spring.jpa.hibernate.ddl-auto` is `validate` — Flyway alone owns the schema. Add new tables via
new `V{n}__description.sql` files rather than editing existing ones.

## Environment Variables

See [`.env.example`](./.env.example) for the full list: database connection, JWT access/refresh
token secrets and expiry, account-lockout thresholds, password-reset/email-verification token TTLs,
CORS origins, and SMTP settings. `MAIL_ADMIN_NOTIFICATIONS` sets where new-booking notifications go
(separate from `MAIL_FROM`, so a real deployment can route them to a staffed inbox), and
`CHILD_PRICE_PERCENT` sets a child's share of the adult package rate (default 70). The
`STORAGE_*`, `IMAGE_*` and `MULTIPART_*` variables configure media uploads — note that
`STORAGE_PUBLIC_BASE_URL` must be an absolute URL the browser can reach, since uploaded images are
rendered by the frontend on a different origin. The `MAIL_*` variables cover the sender identity,
where admin notifications go, the reply-to and support addresses, and `MAIL_ENABLED` to suppress
delivery entirely.

## Docker

```bash
docker build -t tourpackage-api .
docker run -p 8080:8080 --env-file .env tourpackage-api
```

Or run the full stack (web + api + Postgres + Mailhog) via the [root `docker-compose.yml`](../docker-compose.yml).
