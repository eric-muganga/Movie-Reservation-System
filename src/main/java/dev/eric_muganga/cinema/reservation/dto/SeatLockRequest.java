package dev.eric_muganga.cinema.reservation.dto;

import java.util.List;

public record SeatLockRequest(
        Long showtimeId,
        List<Long> seatIds
) { }
