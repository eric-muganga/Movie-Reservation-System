package dev.eric_muganga.cinema.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminReservationView(
        Long reservationId,
        String customerEmail,
        String movieTitle,
        OffsetDateTime showtimeStart,
        String status,
        BigDecimal totalAmount,
        OffsetDateTime createdAt
) {
}
