package dev.eric_muganga.cinema.reservation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateReservationRequest(
        @NotNull Long showtimeId,
        @NotEmpty List<Long> seatIds
) {}