-- Bookings and their child records. A booking sells either a tour package
-- or a hotel room (booking_type discriminates which FK is populated);
-- travellers and payments are 1-to-many children.

CREATE TABLE bookings (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_number      VARCHAR(30)   NOT NULL
        DEFAULT ('TP-' || to_char(now(), 'YYYY') || '-' || lpad(nextval('booking_reference_seq')::text, 6, '0')),
    user_id             UUID          NOT NULL,
    booking_type        VARCHAR(10)   NOT NULL,
    package_id          UUID,
    hotel_room_id       UUID,
    travel_date         DATE          NOT NULL,
    return_date         DATE,
    number_of_adults    SMALLINT      NOT NULL DEFAULT 1,
    number_of_children  SMALLINT      NOT NULL DEFAULT 0,
    total_amount        NUMERIC(12,2) NOT NULL,
    currency_code       VARCHAR(3)       NOT NULL DEFAULT 'USD',
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    special_requests    TEXT,
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(500),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_package FOREIGN KEY (package_id)
        REFERENCES tour_packages (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_hotel_room FOREIGN KEY (hotel_room_id)
        REFERENCES hotel_rooms (id) ON DELETE RESTRICT,
    CONSTRAINT uq_bookings_booking_number UNIQUE (booking_number),
    CONSTRAINT ck_bookings_type CHECK (booking_type IN ('PACKAGE', 'HOTEL')),
    -- The FK populated must match the declared type; the other stays NULL.
    CONSTRAINT ck_bookings_type_target CHECK (
        (booking_type = 'PACKAGE' AND package_id IS NOT NULL AND hotel_room_id IS NULL) OR
        (booking_type = 'HOTEL'   AND hotel_room_id IS NOT NULL AND package_id IS NULL)
    ),
    CONSTRAINT ck_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT ck_bookings_adults        CHECK (number_of_adults >= 0),
    CONSTRAINT ck_bookings_children      CHECK (number_of_children >= 0),
    CONSTRAINT ck_bookings_party_size    CHECK (number_of_adults + number_of_children > 0),
    CONSTRAINT ck_bookings_total_amount  CHECK (total_amount >= 0),
    CONSTRAINT ck_bookings_currency      CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_bookings_return_date   CHECK (return_date IS NULL OR return_date >= travel_date),
    CONSTRAINT ck_bookings_cancellation  CHECK (
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL) OR
        (status != 'CANCELLED' AND cancelled_at IS NULL)
    )
);

CREATE INDEX idx_bookings_user_id       ON bookings (user_id);
CREATE INDEX idx_bookings_package_id    ON bookings (package_id) WHERE package_id IS NOT NULL;
CREATE INDEX idx_bookings_hotel_room_id ON bookings (hotel_room_id) WHERE hotel_room_id IS NOT NULL;
CREATE INDEX idx_bookings_status        ON bookings (status);
CREATE INDEX idx_bookings_travel_date   ON bookings (travel_date);
-- Backs the admin dashboard's "my recent bookings" and status-filtered lists.
CREATE INDEX idx_bookings_user_status_created ON bookings (user_id, status, created_at DESC);

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE booking_travellers (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id        UUID        NOT NULL,
    full_name         VARCHAR(150) NOT NULL,
    date_of_birth     DATE,
    gender            VARCHAR(20),
    passport_number   VARCHAR(50),
    passport_expiry   DATE,
    nationality_country_id UUID,
    is_lead_traveller BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_booking_travellers_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_travellers_nationality FOREIGN KEY (nationality_country_id)
        REFERENCES countries (id) ON DELETE SET NULL,
    CONSTRAINT ck_booking_travellers_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'PREFER_NOT_TO_SAY'))
);

CREATE INDEX idx_booking_travellers_booking_id ON booking_travellers (booking_id);
-- At most one lead traveller per booking.
CREATE UNIQUE INDEX uq_booking_travellers_one_lead ON booking_travellers (booking_id) WHERE is_lead_traveller;

CREATE TRIGGER trg_booking_travellers_updated_at
    BEFORE UPDATE ON booking_travellers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE booking_payments (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id       UUID          NOT NULL,
    amount           NUMERIC(12,2) NOT NULL,
    currency_code    VARCHAR(3)       NOT NULL DEFAULT 'USD',
    payment_method   VARCHAR(30)   NOT NULL,
    provider         VARCHAR(50),
    transaction_id   VARCHAR(100),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    paid_at          TIMESTAMPTZ,
    failure_reason   VARCHAR(500),
    raw_response     JSONB,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_booking_payments_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT uq_booking_payments_transaction_id UNIQUE (transaction_id),
    CONSTRAINT ck_booking_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_booking_payments_currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_booking_payments_method CHECK (payment_method IN
        ('CREDIT_CARD', 'DEBIT_CARD', 'PAYPAL', 'BANK_TRANSFER', 'CASH', 'WALLET')),
    CONSTRAINT ck_booking_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT ck_booking_payments_paid_at CHECK (
        (status = 'SUCCESS' AND paid_at IS NOT NULL) OR
        (status != 'SUCCESS')
    )
);

CREATE INDEX idx_booking_payments_booking_id ON booking_payments (booking_id);
CREATE INDEX idx_booking_payments_status     ON booking_payments (status);

CREATE TRIGGER trg_booking_payments_updated_at
    BEFORE UPDATE ON booking_payments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
