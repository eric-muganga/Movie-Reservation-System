package dev.eric_muganga.cinema.admin.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminReservationDetailView(
        Long reservationId,
        String customerEmail,
        String movieTitle,
        OffsetDateTime showtimeStart,
        String status,
        BigDecimal totalAmount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> seats,
        boolean cancellable
) {
}
