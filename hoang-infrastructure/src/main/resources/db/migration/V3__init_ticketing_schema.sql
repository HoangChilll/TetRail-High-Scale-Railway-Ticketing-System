
-- 1. Ga tàu (stations)
CREATE TABLE IF NOT EXISTS stations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Đoàn tàu (trains)
CREATE TABLE IF NOT EXISTS trains (
    id BIGSERIAL PRIMARY KEY,
    train_number VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Chỗ ngồi trên tàu (seats)
CREATE TABLE IF NOT EXISTS seats (
    id BIGSERIAL PRIMARY KEY,
    train_id BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    seat_number VARCHAR(20) NOT NULL,
    carriage_number VARCHAR(10) NOT NULL,
    seat_class VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_seats_train_seat_number UNIQUE (train_id, seat_number)
);

CREATE INDEX idx_seats_train_seat_class ON seats(train_id, seat_class);

-- 4. Chuyến tàu (trips)
CREATE TABLE IF NOT EXISTS trips (
    id BIGSERIAL PRIMARY KEY,
    train_id BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    departure_station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    arrival_station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    departure_time TIMESTAMPTZ NOT NULL,
    arrival_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trips_dep_arr_time ON trips(departure_station_id, arrival_station_id, departure_time);
CREATE INDEX idx_trips_train_dep_time ON trips(train_id, departure_time);
CREATE INDEX idx_trips_status_dep_time ON trips(status, departure_time);

-- 5. Đơn đặt vé (bookings)
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    buyer_name VARCHAR(150) NOT NULL,
    buyer_email VARCHAR(255) NOT NULL,
    buyer_phone VARCHAR(20) NOT NULL,
    total_amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status_created_at ON bookings(status, created_at);
CREATE INDEX idx_bookings_status_expires_at ON bookings(status, expires_at);

-- 6. Vé tàu chi tiết (tickets)
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    trip_id BIGINT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    seat_id BIGINT NOT NULL REFERENCES seats(id) ON DELETE CASCADE,
    passenger_name VARCHAR(150) NOT NULL,
    passenger_id VARCHAR(50),
    passenger_type VARCHAR(30),
    departure_station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    arrival_station_id BIGINT NOT NULL REFERENCES stations(id) ON DELETE CASCADE,
    fare NUMERIC(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RESERVED',
    issued_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX idx_tickets_trip_id ON tickets(trip_id);
CREATE INDEX idx_tickets_seat_id ON tickets(seat_id);
CREATE INDEX idx_tickets_trip_seat ON tickets(trip_id, seat_id);
CREATE INDEX idx_tickets_trip_status ON tickets(trip_id, status);

-- 7. Thanh toán đơn đặt vé (payments)
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    payment_code VARCHAR(64) NOT NULL UNIQUE,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    payment_method VARCHAR(30) NOT NULL,
    payment_provider VARCHAR(50),
    provider_transaction_id VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_provider_tx ON payments(payment_provider, provider_transaction_id);
CREATE INDEX idx_payments_status_created_at ON payments(status, created_at);

