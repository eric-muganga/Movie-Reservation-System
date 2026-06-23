package dev.eric_muganga.cinema.showtime.dto;

import java.util.List;

public record SeatRowDto(
        String rowLabel,
        List<SeatInRowDto> seats
) { }
