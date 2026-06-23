package dev.eric_muganga.cinema.showtime.dto;

import java.util.List;

public record ShowtimeSeatingResponse(
        Long showtimeId,
        Long auditoriumId,
        String auditoriumName,
        List<SeatRowDto> rows
) { }
