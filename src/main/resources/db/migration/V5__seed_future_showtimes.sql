-- V5__seed_future_showtimes.sql

DO $$
DECLARE
screen_1_id BIGINT;
BEGIN
SELECT id
INTO screen_1_id
FROM auditoriums
WHERE name = 'Screen 1';

IF screen_1_id IS NULL THEN
        RAISE EXCEPTION 'Screen 1 auditorium does not exist';
END IF;

    -- Day 1: Inception and The Dark Knight
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
    screen_1_id,
    ((CURRENT_DATE + 1) + TIME '18:00')::timestamptz,
        ((CURRENT_DATE + 1) + TIME '20:30')::timestamptz,
        'SCHEDULED',
    12.50
FROM movies m
WHERE m.title = 'Inception'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 1) + TIME '18:00')::timestamptz
);

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
    screen_1_id,
    ((CURRENT_DATE + 1) + TIME '21:00')::timestamptz,
        ((CURRENT_DATE + 1) + TIME '23:45')::timestamptz,
        'SCHEDULED',
    13.50
FROM movies m
WHERE m.title = 'The Dark Knight'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 1) + TIME '21:00')::timestamptz
);

-- Day 2: Interstellar and The Matrix
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
    screen_1_id,
    ((CURRENT_DATE + 2) + TIME '18:00')::timestamptz,
        ((CURRENT_DATE + 2) + TIME '21:00')::timestamptz,
        'SCHEDULED',
    14.50
FROM movies m
WHERE m.title = 'Interstellar'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 2) + TIME '18:00')::timestamptz
);

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
    screen_1_id,
    ((CURRENT_DATE + 2) + TIME '21:15')::timestamptz,
        ((CURRENT_DATE + 2) + TIME '23:45')::timestamptz,
        'SCHEDULED',
    12.50
FROM movies m
WHERE m.title = 'The Matrix'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 2) + TIME '21:15')::timestamptz
);

-- Day 3: Toy Story, La La Land, Parasite
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
    screen_1_id,
    ((CURRENT_DATE + 3) + TIME '14:00')::timestamptz,
        ((CURRENT_DATE + 3) + TIME '15:30')::timestamptz,
        'SCHEDULED',
    10.00
FROM movies m
WHERE m.title = 'Toy Story'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 3) + TIME '14:00')::timestamptz
);

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
    screen_1_id,
    ((CURRENT_DATE + 3) + TIME '17:00')::timestamptz,
        ((CURRENT_DATE + 3) + TIME '19:30')::timestamptz,
        'SCHEDULED',
    12.00
FROM movies m
WHERE m.title = 'La La Land'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 3) + TIME '17:00')::timestamptz
);

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
    screen_1_id,
    ((CURRENT_DATE + 3) + TIME '20:00')::timestamptz,
        ((CURRENT_DATE + 3) + TIME '22:30')::timestamptz,
        'SCHEDULED',
    13.00
FROM movies m
WHERE m.title = 'Parasite'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 3) + TIME '20:00')::timestamptz
);

-- Day 4: John Wick
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
    screen_1_id,
    ((CURRENT_DATE + 4) + TIME '19:00')::timestamptz,
        ((CURRENT_DATE + 4) + TIME '21:00')::timestamptz,
        'SCHEDULED',
    13.50
FROM movies m
WHERE m.title = 'John Wick'
  AND NOT EXISTS (
    SELECT 1
    FROM showtimes s
    WHERE s.movie_id = m.id
      AND s.auditorium_id = screen_1_id
      AND s.start_time = ((CURRENT_DATE + 4) + TIME '19:00')::timestamptz
);
END $$;