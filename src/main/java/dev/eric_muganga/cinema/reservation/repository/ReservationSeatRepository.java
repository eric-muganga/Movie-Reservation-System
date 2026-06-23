package dev.eric_muganga.cinema.reservation.repository;

import dev.eric_muganga.cinema.reservation.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
    List<ReservationSeat> findByReservationId(Long reservationId);

    @Query("""
        SELECT rs 
        FROM ReservationSeat rs
        WHERE rs.showtime.id=:showtimeId
    """
    )
    List<ReservationSeat> findByShowtimeId(@Param("showtimeId") Long showtimeId);

    @Query("""
       SELECT rs.seat.id
       FROM ReservationSeat rs
       WHERE rs.showtime.id = :showtimeId
         AND rs.seat.id IN :seatIds
         AND rs.reservation.status = dev.eric_muganga.cinema.reservation.entity.ReservationStatus.CONFIRMED
       """)
    List<Long> findReservedSeatIdsForShowtime(Long showtimeId, List<Long> seatIds);

    @Query("""
       SELECT rs
       FROM ReservationSeat rs
       WHERE rs.showtime.id = :showtimeId
         AND rs.reservation.status = dev.eric_muganga.cinema.reservation.entity.ReservationStatus.CONFIRMED
       """)
    List<ReservationSeat> findActiveByShowtimeId(Long showtimeId);
}
