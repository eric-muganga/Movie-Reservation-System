package dev.eric_muganga.cinema.reservation.controller;

import dev.eric_muganga.cinema.reservation.dto.PagedShowtimeReportResponse;
import dev.eric_muganga.cinema.reservation.dto.ShowtimeReport;
import dev.eric_muganga.cinema.reservation.service.IAdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {
    private final IAdminReportService adminReportService;

    /**
     * Dashboard card for a single showtime:
     * occupancy and revenue for one screen/session.
     */
    @GetMapping("/showtimes/{showtimeId}")
    public ResponseEntity<ShowtimeReport> getShowtimeReport(
            @PathVariable Long showtimeId
    ) {
        ShowtimeReport report = adminReportService.getShowtimeReport(showtimeId);
        return ResponseEntity.ok(report);
    }

    /**
     * Dashboard view for a given business date:
     * occupancy and revenue across all showtimes and screens.
     * Example: GET /api/admin/reports/showtimes?date=2026-07-01
     */
    @GetMapping("/showtimes")
    public ResponseEntity<PagedShowtimeReportResponse> getShowtimeReportsForDate(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PagedShowtimeReportResponse response =
                adminReportService.getShowtimeReportsForDate(date, pageable);

        return ResponseEntity.ok(response);
    }
}
