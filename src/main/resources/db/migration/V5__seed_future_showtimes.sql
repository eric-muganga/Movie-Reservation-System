-- V5__seed_future_showtimes.sql
--
-- Development/demo showtimes.
-- All schedules use Screen 1 and are placed 7–10 days in the future.
-- PostgreSQL prevents overlapping screenings through the
-- no_overlapping_showtimes exclusion constraint.

-- Day 7: Inception
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 7) + TIME '18:00')::timestamptz,
        ((CURRENT_DATE + 7) + TIME '20:30')::timestamptz,
        'SCHEDULED',
    12.50
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'Inception'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 7: The Dark Knight
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 7) + TIME '21:00')::timestamptz,
        ((CURRENT_DATE + 7) + TIME '23:45')::timestamptz,
        'SCHEDULED',
    13.50
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'The Dark Knight'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 8: Interstellar
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 8) + TIME '18:00')::timestamptz,
        ((CURRENT_DATE + 8) + TIME '21:00')::timestamptz,
        'SCHEDULED',
    14.50
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'Interstellar'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 8: The Matrix
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 8) + TIME '21:15')::timestamptz,
        ((CURRENT_DATE + 8) + TIME '23:45')::timestamptz,
        'SCHEDULED',
    12.50
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'The Matrix'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 9: Toy Story
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 9) + TIME '14:00')::timestamptz,
        ((CURRENT_DATE + 9) + TIME '15:30')::timestamptz,
        'SCHEDULED',
    10.00
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'Toy Story'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 9: La La Land
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 9) + TIME '17:00')::timestamptz,
        ((CURRENT_DATE + 9) + TIME '19:30')::timestamptz,
        'SCHEDULED',
    12.00
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'La La Land'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 9: Parasite
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 9) + TIME '20:00')::timestamptz,
        ((CURRENT_DATE + 9) + TIME '22:30')::timestamptz,
        'SCHEDULED',
    13.00
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'Parasite'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;

-- Day 10: John Wick
INSERT INTO showtimes (
    movie_id,
    auditorium_id,
    start_time,
    end_time,
    status,
    base_price
)
SELECT
    m.id,
    a.id,
    ((CURRENT_DATE + 10) + TIME '19:00')::timestamptz,
        ((CURRENT_DATE + 10) + TIME '21:00')::timestamptz,
        'SCHEDULED',
    13.50
FROM movies m
         JOIN auditoriums a ON a.name = 'Screen 1'
WHERE m.title = 'John Wick'
    ON CONFLICT ON CONSTRAINT no_overlapping_showtimes DO NOTHING;