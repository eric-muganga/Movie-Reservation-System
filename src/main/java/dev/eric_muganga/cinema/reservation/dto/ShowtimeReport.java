package dev.eric_muganga.cinema.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ShowtimeReport(
        Long showtimeId,
        String movieTitle,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String auditoriumName,
        int totalSeats,
        int reservedSeats,
        double capacityPercent,
        BigDecimal totalRevenue
) { }
