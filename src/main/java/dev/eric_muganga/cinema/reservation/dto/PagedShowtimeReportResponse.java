package dev.eric_muganga.cinema.reservation.dto;

import java.util.List;

public record PagedShowtimeReportResponse(
        List<ShowtimeReport> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {}