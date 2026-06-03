-------------------------- Users and roles

-- USERS
CREATE TABLE users (
                       id            BIGSERIAL PRIMARY KEY,
                       auth0_sub     VARCHAR(255) UNIQUE NOT NULL, -- maps to Auth0 user id (sub)
                       email         VARCHAR(255) UNIQUE NOT NULL,
                       full_name     VARCHAR(255) NOT NULL,
                       created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ROLES (ADMIN, USER, ...)
CREATE TABLE roles (
                       id        BIGSERIAL PRIMARY KEY,
                       name      VARCHAR(50) UNIQUE NOT NULL
);

-- USER_ROLES (many-to-many)
CREATE TABLE user_roles (
                            user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id   BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            PRIMARY KEY (user_id, role_id)
);

-- Seed basic roles
INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');


--------------------------------------Movies and genres
-- GENRES
CREATE TABLE genres (
                        id        BIGSERIAL PRIMARY KEY,
                        name      VARCHAR(100) UNIQUE NOT NULL
);

-- MOVIES
CREATE TABLE movies (
                        id              BIGSERIAL PRIMARY KEY,
                        title           VARCHAR(255) NOT NULL,
                        description     TEXT NOT NULL,
                        poster_url      VARCHAR(500),
                        duration_minutes INT NOT NULL,
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- MOVIE_GENRES (many-to-many)
CREATE TABLE movie_genres (
                              movie_id  BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                              genre_id  BIGINT NOT NULL REFERENCES genres(id) ON DELETE RESTRICT,
                              PRIMARY KEY (movie_id, genre_id)
);


--------------------------------------Auditoriums, seats, showtimes

-- AUDITORIUMS
CREATE TABLE auditoriums (
                             id          BIGSERIAL PRIMARY KEY,
                             name        VARCHAR(100) NOT NULL UNIQUE,
                             total_rows  INT NOT NULL,
                             total_cols  INT NOT NULL
);

-- SEATS (physical seats in an auditorium)
CREATE TABLE seats (
                       id             BIGSERIAL PRIMARY KEY,
                       auditorium_id  BIGINT NOT NULL REFERENCES auditoriums(id) ON DELETE CASCADE,
                       row_label      VARCHAR(10) NOT NULL,
                       seat_number    INT NOT NULL,
                       seat_type      VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
                       CONSTRAINT uq_seat_per_aud UNIQUE (auditorium_id, row_label, seat_number)
);

-- SHOWTIMES
CREATE TABLE showtimes (
                           id             BIGSERIAL PRIMARY KEY,
                           movie_id       BIGINT NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                           auditorium_id  BIGINT NOT NULL REFERENCES auditoriums(id) ON DELETE RESTRICT,
                           start_time     TIMESTAMPTZ NOT NULL,
                           end_time       TIMESTAMPTZ NOT NULL,
                           status         VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                           base_price     NUMERIC(10,2) NOT NULL,
    -- stored range, derived from start_time / end_time
                           time_range     TSTZRANGE GENERATED ALWAYS AS (
                               tstzrange(start_time, end_time, '[)')
                               ) STORED
);

-- basic index to fetch showtimes by date
CREATE INDEX idx_showtimes_start_time ON showtimes (start_time);

-- prevent overlapping showtimes in the same auditorium
-- requires btree_gist extension for gist on bigint + range
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE showtimes
    ADD CONSTRAINT no_overlapping_showtimes
    EXCLUDE USING gist (
        auditorium_id WITH =,
        time_range    WITH &&
    );


-----------------------------------------Reservations and reservation_seats

-- RESERVATIONS
CREATE TABLE reservations (
                              id             BIGSERIAL PRIMARY KEY,
                              user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                              showtime_id    BIGINT NOT NULL REFERENCES showtimes(id) ON DELETE RESTRICT,
                              status         VARCHAR(20) NOT NULL, -- e.g. PENDING, CONFIRMED, CANCELLED
                              total_amount   NUMERIC(10,2) NOT NULL DEFAULT 0,
                              created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                              updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- For quickly finding a user's reservations
CREATE INDEX idx_reservations_user ON reservations (user_id, created_at DESC);

-- RESERVATION_SEATS
CREATE TABLE reservation_seats (
                                   id              BIGSERIAL PRIMARY KEY,
                                   reservation_id  BIGINT NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
                                   seat_id         BIGINT NOT NULL REFERENCES seats(id) ON DELETE RESTRICT,
                                   price           NUMERIC(10,2) NOT NULL
);

-- A seat can only belong to one active reservation per showtime.
-- We enforce uniqueness at (showtime, seat) via a functional unique index.
-- First: ensure showtime_id is accessible.
-- Simplest: add showtime_id directly here (denormalized):

ALTER TABLE reservation_seats
    ADD COLUMN showtime_id BIGINT NOT NULL REFERENCES showtimes(id) ON DELETE RESTRICT;

-- Then enforce unique seat per showtime
CREATE UNIQUE INDEX uq_reservation_seats_showtime_seat
    ON reservation_seats (showtime_id, seat_id);


-- SEAT_LOCKS: temporary holds on seats for a given showtime
CREATE TABLE seat_locks (
                            id              BIGSERIAL PRIMARY KEY,
                            user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            showtime_id     BIGINT NOT NULL REFERENCES showtimes(id) ON DELETE CASCADE,
                            seat_id         BIGINT NOT NULL REFERENCES seats(id) ON DELETE CASCADE,
                            locked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                            lock_expires_at TIMESTAMPTZ NOT NULL,
    -- optionally track status if you want to mark as CONSUMED, EXPIRED, etc.
                            status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE INDEX idx_seat_locks_showtime_active
    ON seat_locks (showtime_id, lock_expires_at)
    WHERE status = 'ACTIVE';

-- Only one ACTIVE lock per seat+showtime
CREATE UNIQUE INDEX uq_seat_locks_active
    ON seat_locks (showtime_id, seat_id)
    WHERE status = 'ACTIVE';