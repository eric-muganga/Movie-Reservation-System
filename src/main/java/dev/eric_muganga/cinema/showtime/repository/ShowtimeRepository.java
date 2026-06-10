package dev.eric_muganga.cinema.showtime.repository;

import dev.eric_muganga.cinema.showtime.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    // All showtimes for a movie in a time range (e.g. one day)
    List<Showtime> findByMovieIdAndStartTimeBetween(
            Long movieId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    // All showtimes in a time range
    List<Showtime> findByStartTimeBetween(
            OffsetDateTime from,
            OffsetDateTime to
    );

    // Optional: paged variant for UIs
    Page<Showtime> findByStartTimeBetween(
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );
}
