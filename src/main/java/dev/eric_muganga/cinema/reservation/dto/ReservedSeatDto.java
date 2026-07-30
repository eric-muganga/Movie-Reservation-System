package dev.eric_muganga.cinema.reservation.dto;

import java.math.BigDecimal;

public record ReservedSeatDto(
        Long seatId,
        String rowLabel,
        int seatNumber,
        BigDecimal price
) {
}
