package dev.eric_muganga.cinema.showtime.dto;

public record SeatInRowDto(
        Long seatId,
        int seatNumber,
        SeatStatus status,
        boolean wheelchairAccessible
) { }
