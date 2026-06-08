package dev.eric_muganga.cinema.reservation.repository;

import dev.eric_muganga.cinema.reservation.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, Long> {
    List<ReservationSeat> findByReservationId(Long reservationId);
    List<ReservationSeat> findByShowtimeId(Long showtimeId);
}
