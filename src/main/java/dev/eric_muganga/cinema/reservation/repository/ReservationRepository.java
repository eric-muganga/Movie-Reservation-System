package dev.eric_muganga.cinema.reservation.repository;

import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Reservation> findByShowtimeId(Long showtimeId);
    List<Reservation> findByShowtimeIdAndStatus(Long showtimeId, ReservationStatus status);
    List<Reservation> findByUserIdAndShowtime_StartTimeAfterOrderByShowtime_StartTimeAsc(
            Long userId,
            OffsetDateTime now
    );
}
