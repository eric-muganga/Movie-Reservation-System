package dev.eric_muganga.cinema.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SeatLockRequest(
        @NotNull(message = "Showtime is required")
        Long showtimeId,

        @NotEmpty(message = "At least one seat must be selected")
        List<@NotNull(message = "Seat ID is required") Long> seatIds
) { }
