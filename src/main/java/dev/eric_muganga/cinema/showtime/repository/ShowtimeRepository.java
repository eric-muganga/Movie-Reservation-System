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
    List<Showtime> findByMovieIdAndStartTimeBetween(
            Long movieId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    List<Showtime> findByStartTimeBetween(
            OffsetDateTime from,
            OffsetDateTime to
    );

    Page<Showtime> findByStartTimeBetween(
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    List<Showtime> findAllByOrderByStartTimeAsc();

    boolean existsByAuditoriumIdAndStartTimeLessThanAndEndTimeGreaterThan(
            Long auditoriumId,
            OffsetDateTime endTime,
            OffsetDateTime startTime
    );

    boolean existsByAuditoriumIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
            Long auditoriumId,
            Long showtimeId,
            OffsetDateTime endTime,
            OffsetDateTime startTime
    );
}
