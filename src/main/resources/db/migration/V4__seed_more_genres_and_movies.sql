INSERT INTO genres (name) VALUES
                              ('Action'),
                              ('Drama'),
                              ('Sci-Fi'),
                              ('Comedy'),
                              ('Thriller'),
                              ('Romance'),
                              ('Adventure'),
                              ('Animation'),
                              ('Horror'),
                              ('Crime')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO movies (title, description, poster_url, duration_minutes, created_at, updated_at)
VALUES
    ('Inception', 'A thief who steals corporate secrets through dream-sharing technology.', 'https://example.com/inception.jpg', 148, NOW(), NOW()),
    ('The Dark Knight', 'Batman faces the Joker in Gotham City.', 'https://example.com/dark-knight.jpg', 152, NOW(), NOW()),
    ('Interstellar', 'A team travels through a wormhole in space to save humanity.', 'https://example.com/interstellar.jpg', 169, NOW(), NOW()),
    ('The Matrix', 'A hacker discovers reality is a simulation.', 'https://example.com/matrix.jpg', 136, NOW(), NOW()),
    ('Toy Story', 'A group of toys come to life when humans are not around.', 'https://example.com/toy-story.jpg', 81, NOW(), NOW()),
    ('La La Land', 'A musician and an actress pursue their dreams in Los Angeles.', 'https://example.com/la-la-land.jpg', 128, NOW(), NOW()),
    ('Parasite', 'A poor family infiltrates a wealthy household.', 'https://example.com/parasite.jpg', 132, NOW(), NOW()),
    ('John Wick', 'An ex-hitman comes out of retirement for revenge.', 'https://example.com/john-wick.jpg', 101, NOW(), NOW())
    ON CONFLICT DO NOTHING;

-- Inception -> Sci-Fi, Thriller
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Sci-Fi'
WHERE m.title = 'Inception'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Thriller'
WHERE m.title = 'Inception'
    ON CONFLICT DO NOTHING;

-- The Dark Knight -> Action, Crime
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Action'
WHERE m.title = 'The Dark Knight'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Crime'
WHERE m.title = 'The Dark Knight'
    ON CONFLICT DO NOTHING;

-- Interstellar -> Sci-Fi, Adventure, Drama
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Sci-Fi'
WHERE m.title = 'Interstellar'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Adventure'
WHERE m.title = 'Interstellar'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'Interstellar'
    ON CONFLICT DO NOTHING;

-- The Matrix -> Sci-Fi, Action
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Sci-Fi'
WHERE m.title = 'The Matrix'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Action'
WHERE m.title = 'The Matrix'
    ON CONFLICT DO NOTHING;

-- Toy Story -> Animation, Comedy
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Animation'
WHERE m.title = 'Toy Story'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Comedy'
WHERE m.title = 'Toy Story'
    ON CONFLICT DO NOTHING;

-- La La Land -> Romance, Drama, Comedy
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Romance'
WHERE m.title = 'La La Land'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'La La Land'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Comedy'
WHERE m.title = 'La La Land'
    ON CONFLICT DO NOTHING;

-- Parasite -> Drama, Thriller, Comedy
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Drama'
WHERE m.title = 'Parasite'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Thriller'
WHERE m.title = 'Parasite'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Comedy'
WHERE m.title = 'Parasite'
    ON CONFLICT DO NOTHING;

-- John Wick -> Action, Thriller, Crime
INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Action'
WHERE m.title = 'John Wick'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Thriller'
WHERE m.title = 'John Wick'
    ON CONFLICT DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT m.id, g.id
FROM movies m
         JOIN genres g ON g.name = 'Crime'
WHERE m.title = 'John Wick'
    ON CONFLICT DO NOTHING;