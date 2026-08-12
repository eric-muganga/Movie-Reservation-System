package dev.eric_muganga.cinema.showtime.dto;

import dev.eric_muganga.cinema.showtime.entity.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminShowtimeView(
        Long id,
        Long movieId,
        String movieTitle,
        Long auditoriumId,
        String auditoriumName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        ShowtimeStatus status,
        BigDecimal basePrice
) {
}
