package dev.eric_muganga.cinema.venue.repository;

import dev.eric_muganga.cinema.venue.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByAuditorium_Id(Long auditoriumId);
    List<Seat> findByAuditoriumIdAndSeatType(Long auditoriumId, String seatType);
    List<Seat> findByAuditorium_IdOrderByRowLabelAscSeatNumberAsc(Long auditoriumId);
    int countByAuditorium_Id(Long auditoriumId);
}
