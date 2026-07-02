package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.reservation.dto.ShowtimeReport;

import java.time.LocalDate;
import java.util.List;

public interface IAdminReportService {
    ShowtimeReport getShowtimeReport(Long showtimeId);
    List<ShowtimeReport> getShowtimeReportsForDate(LocalDate businessDate);
}
