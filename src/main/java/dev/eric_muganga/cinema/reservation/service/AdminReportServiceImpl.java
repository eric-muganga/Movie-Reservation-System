package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.reservation.dto.PagedShowtimeReportResponse;
import dev.eric_muganga.cinema.reservation.dto.ShowtimeReport;
import dev.eric_muganga.cinema.reservation.repository.ReservationRepository;
import dev.eric_muganga.cinema.reservation.repository.ReservationSeatRepository;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.showtime.repository.ShowtimeRepository;
import dev.eric_muganga.cinema.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements IAdminReportService {
    private final ShowtimeRepository showtimeRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;


    @Override
    public ShowtimeReport getShowtimeReport(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + showtimeId));

        Long auditoriumId = showtime.getAuditorium().getId();

        long totalSeats = seatRepository.countByAuditorium_Id(auditoriumId);
        long reservedSeats = reservationSeatRepository.countByShowtime_Id(showtimeId);
        double capacityPercent = totalSeats == 0 ? 0.0 :
                (reservedSeats * 100.0) / totalSeats;

        BigDecimal totalRevenue = reservationRepository.sumTotalAmountByShowtimeId(showtimeId);

        return new ShowtimeReport(
                showtime.getId(),
                showtime.getMovie().getTitle(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getAuditorium().getName(),
                (int) totalSeats,
                (int) reservedSeats,
                capacityPercent,
                totalRevenue
        );
    }

    @Override
    public PagedShowtimeReportResponse getShowtimeReportsForDate(LocalDate businessDate, Pageable pageable) {
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime start = businessDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = businessDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        Page<Showtime> showtimePage =
                showtimeRepository.findByStartTimeBetween(start, end, pageable);

        List<ShowtimeReport> reports = showtimePage
                .getContent()
                .stream()
                .map(this::buildShowtimeReport)
                .toList();

        return new PagedShowtimeReportResponse(
                reports,
                showtimePage.getNumber(),
                showtimePage.getSize(),
                showtimePage.getTotalElements(),
                showtimePage.getTotalPages()
        );
    }

    private ShowtimeReport buildShowtimeReport(Showtime showtime) {
        Long showtimeId = showtime.getId();
        Long auditoriumId = showtime.getAuditorium().getId();

        long totalSeats = seatRepository.countByAuditorium_Id(auditoriumId);
        long reservedSeats = reservationSeatRepository.countByShowtime_Id(showtimeId);
        double capacityPercent = totalSeats == 0 ? 0.0 :
                (reservedSeats * 100.0) / totalSeats;

        BigDecimal totalRevenue = reservationRepository.sumTotalAmountByShowtimeId(showtimeId);

        return new ShowtimeReport(
                showtimeId,
                showtime.getMovie().getTitle(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getAuditorium().getName(),
                (int) totalSeats,
                (int) reservedSeats,
                capacityPercent,
                totalRevenue
        );
    }
}
