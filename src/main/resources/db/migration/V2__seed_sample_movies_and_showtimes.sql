
-- Seed genres
INSERT INTO genres (name) VALUES
                              ('Action'),
                              ('Drama'),
                              ('Sci-Fi')
    ON CONFLICT (name) DO NOTHING;

-- Seed movies
INSERT INTO movies (title, description, poster_url, duration_minutes)
VALUES
    ('Inception', 'A thief who steals corporate secrets through dream-sharing technology.', 'https://example.com/inception.jpg', 148),
    ('The Dark Knight', 'Batman faces the Joker, a criminal mastermind who plunges Gotham into chaos.', 'https://example.com/dark-knight.jpg', 152)
    RETURNING id, title;

-- Capture generated ids (only if you run this manually);
-- In Flyway SQL migrations, we usually rely on known sequences.
-- For simplicity, assume this is the first seed and these movies get ids 1 and 2.

-- Link movies to genres (assuming ids from previous inserts)
-- Inception -> Sci-Fi
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Sci-Fi'
WHERE m.title = 'Inception'
    ON CONFLICT DO NOTHING;

-- The Dark Knight -> Action, Drama
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Action'
WHERE m.title = 'The Dark Knight'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'The Dark Knight'
    ON CONFLICT DO NOTHING;

-- Seed a simple auditorium
INSERT INTO auditoriums (name, total_rows, total_cols)
VALUES ('Screen 1', 5, 10)
    ON CONFLICT (name) DO NOTHING;

-- Seed seats for Screen 1: rows A–E, seats 1–10
-- (If this runs more than once, ON CONFLICT prevents duplicates)
DO $$
DECLARE
aud_id BIGINT;
    row_label TEXT;
    seat_num INT;
BEGIN
SELECT id INTO aud_id FROM auditoriums WHERE name = 'Screen 1';

IF aud_id IS NOT NULL THEN
        FOR row_label IN SELECT unnest(ARRAY['A','B','C','D','E'])
                                       LOOP
                                    FOR seat_num IN 1..10 LOOP
                         INSERT INTO seats (auditorium_id, row_label, seat_number, seat_type)
                         VALUES (aud_id, row_label, seat_num, 'STANDARD')
                         ON CONFLICT ON CONSTRAINT uq_seat_per_aud DO NOTHING;
END LOOP;
END LOOP;
END IF;
END $$;

-- Seed showtimes for a specific date
-- Example: today at 18:00 and 21:00 for Inception,
-- and tomorrow at 19:00 for The Dark Knight.
DO $$
DECLARE
aud_id BIGINT;
    inception_id BIGINT;
    tdk_id BIGINT;
BEGIN
SELECT id INTO aud_id FROM auditoriums WHERE name = 'Screen 1';
SELECT id INTO inception_id FROM movies WHERE title = 'Inception';
SELECT id INTO tdk_id FROM movies WHERE title = 'The Dark Knight';

IF aud_id IS NOT NULL AND inception_id IS NOT NULL THEN
        INSERT INTO showtimes (movie_id, auditorium_id, start_time, end_time, status, base_price)
        VALUES
            (
                inception_id,
                aud_id,
                (now()::date + time '18:00')::timestamptz,
                (now()::date + time '20:30')::timestamptz,
                'SCHEDULED',
                12.50
            ),
            (
                inception_id,
                aud_id,
                (now()::date + time '21:00')::timestamptz,
                (now()::date + time '23:30')::timestamptz,
                'SCHEDULED',
                12.50
            );
END IF;

    IF aud_id IS NOT NULL AND tdk_id IS NOT NULL THEN
        INSERT INTO showtimes (movie_id, auditorium_id, start_time, end_time, status, base_price)
        VALUES
            (
                tdk_id,
                aud_id,
                ((now()::date + 1) + time '19:00')::timestamptz,
                ((now()::date + 1) + time '21:45')::timestamptz,
                'SCHEDULED',
                13.50
            );
END IF;
END $$;