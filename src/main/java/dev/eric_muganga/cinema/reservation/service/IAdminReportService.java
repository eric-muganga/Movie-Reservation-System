package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.reservation.dto.PagedShowtimeReportResponse;
import dev.eric_muganga.cinema.reservation.dto.ShowtimeReport;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface IAdminReportService {
    ShowtimeReport getShowtimeReport(Long showtimeId);
    PagedShowtimeReportResponse getShowtimeReportsForDate(LocalDate date, Pageable pageable);
}
