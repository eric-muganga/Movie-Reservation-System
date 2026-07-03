package dev.eric_muganga.cinema.reservation.repository;

import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Reservation> findByShowtimeId(Long showtimeId);
    List<Reservation> findByShowtimeIdAndStatus(Long showtimeId, ReservationStatus status);
    List<Reservation> findByUserIdAndShowtime_StartTimeAfterOrderByShowtime_StartTimeAsc(
            Long userId,
            OffsetDateTime now
    );

    @Query("""
        select coalesce(sum(r.totalAmount), 0)
        from Reservation r
        where r.showtime.id = :showtimeId
          and r.status = dev.eric_muganga.cinema.reservation.entity.ReservationStatus.CONFIRMED
    """)
    BigDecimal sumTotalAmountByShowtimeId( @Param("showtimeId") Long showtimeId);
}
