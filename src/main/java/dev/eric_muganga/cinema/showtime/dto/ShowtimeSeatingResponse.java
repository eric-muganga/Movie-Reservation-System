package dev.eric_muganga.cinema.showtime.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShowtimeSeatingResponse(
        Long showtimeId,
        Long auditoriumId,
        String auditoriumName,
        BigDecimal basePrice,
        List<SeatRowDto> rows
) { }
