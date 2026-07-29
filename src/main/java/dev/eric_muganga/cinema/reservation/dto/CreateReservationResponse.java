package dev.eric_muganga.cinema.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateReservationResponse(
        Long reservationId,
        String reservationStatus,
        String paymentStatus,
        BigDecimal totalAmount,
        OffsetDateTime paymentExpiresAt,
        List<String> seatLabels,
        String message
) {}
