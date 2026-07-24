package dev.eric_muganga.cinema.admin.dto;

public record AdminReservationActionResult(
        Long reservationId,
        String status,
        String message
) { }
