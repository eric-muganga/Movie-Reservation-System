package dev.eric_muganga.cinema.showtime.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.reservation.entity.ReservationSeat;
import dev.eric_muganga.cinema.reservation.entity.SeatLock;
import dev.eric_muganga.cinema.reservation.repository.ReservationSeatRepository;
import dev.eric_muganga.cinema.reservation.repository.SeatLockRepository;
import dev.eric_muganga.cinema.showtime.dto.SeatInRowDto;
import dev.eric_muganga.cinema.showtime.dto.SeatRowDto;
import dev.eric_muganga.cinema.showtime.dto.SeatStatus;
import dev.eric_muganga.cinema.showtime.dto.ShowtimeSeatingResponse;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.showtime.repository.ShowtimeRepository;
import dev.eric_muganga.cinema.venue.entity.Seat;
import dev.eric_muganga.cinema.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatingServiceImpl implements ISeatingService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    @Override
    public ShowtimeSeatingResponse getSeatingForShowtime(Long showtimeId, String auth0Sub) {
        // 1) Load showtime
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Showtime not found: " + showtimeId
                ));

        Long auditoriumId = showtime.getAuditorium().getId();
        String auditoriumName = showtime.getAuditorium().getName();

        // 2) Load all seats in auditorium
        List<Seat> seats = seatRepository
                .findByAuditorium_IdOrderByRowLabelAscSeatNumberAsc(auditoriumId);

        if (seats.isEmpty()) {
            // Edge case: auditorium misconfigured, but respond gracefully
            return new ShowtimeSeatingResponse(
                    showtimeId,
                    auditoriumId,
                    auditoriumName,
                    List.of()
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 3) Load active locks
        List<SeatLock> activeLocks = seatLockRepository
                .findActiveLocksForShowtime(showtimeId, now);

        // Map seatId -> lock
        Map<Long, SeatLock> lockBySeatId = activeLocks.stream()
                .collect(Collectors.toMap(
                        lock -> lock.getSeat().getId(),
                        lock -> lock,
                        (a, b) -> a
                ));

        // 4) Load reserved seats
        List<ReservationSeat> reservedSeats = reservationSeatRepository
                .findByShowtimeId(showtimeId);

        // Map seatId -> reservationSeat
        Map<Long, ReservationSeat> reservationBySeatId = reservedSeats.stream()
                .collect(Collectors.toMap(
                        rs -> rs.getSeat().getId(),
                        rs -> rs,
                        (a, b) -> a
                ));

        // 5) Build row map: rowLabel -> list of SeatInRowDto
        Map<String, List<SeatInRowDto>> rowsMap = new LinkedHashMap<>();

        for (Seat seat : seats) {
            Long seatId = seat.getId();

            SeatStatus status;
            if (reservationBySeatId.containsKey(seatId)) {
                status = SeatStatus.RESERVED;
            } else if (lockBySeatId.containsKey(seatId)) {
                status = SeatStatus.LOCKED;
            } else {
                status = SeatStatus.AVAILABLE;
            }

            boolean wheelchairAccessible = false; // adjust once you have this on Seat

            SeatInRowDto seatDto = new SeatInRowDto(
                    seatId,
                    seat.getSeatNumber(),
                    status,
                    wheelchairAccessible
            );

            rowsMap
                    .computeIfAbsent(seat.getRowLabel(), key -> new ArrayList<>())
                    .add(seatDto);
        }

        // 6) Convert map to list of SeatRowDto, preserving insertion order
        List<SeatRowDto> rowDtos = rowsMap.entrySet().stream()
                .map(entry -> new SeatRowDto(entry.getKey(), entry.getValue()))
                .toList();

        return new ShowtimeSeatingResponse(
                showtimeId,
                auditoriumId,
                auditoriumName,
                rowDtos
        );
    }
}
