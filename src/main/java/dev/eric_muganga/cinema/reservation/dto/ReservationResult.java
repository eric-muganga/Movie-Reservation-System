package dev.eric_muganga.cinema.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ReservationResult(
        Long reservationId,
        String status,               // e.g. "CONFIRMED"
        Long showtimeId,
        OffsetDateTime createdAt,
        BigDecimal totalAmount,
        List<ReservedSeatDto> seats
) {
}
