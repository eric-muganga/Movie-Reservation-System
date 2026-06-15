package dev.eric_muganga.cinema.movie.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ShowtimeDto(
        Long id,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        BigDecimal basePrice
) {}

