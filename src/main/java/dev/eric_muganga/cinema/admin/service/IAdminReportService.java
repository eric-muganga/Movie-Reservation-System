package dev.eric_muganga.cinema.admin.service;

import dev.eric_muganga.cinema.reservation.dto.PagedShowtimeReportResponse;
import dev.eric_muganga.cinema.reservation.dto.ShowtimeReport;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IAdminReportService {
    ShowtimeReport getShowtimeReport(Long showtimeId);
    PagedShowtimeReportResponse getShowtimeReportsForDate(LocalDate date, Pageable pageable);
}
