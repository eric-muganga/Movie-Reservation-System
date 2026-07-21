package dev.eric_muganga.cinema.admin.ui;

import dev.eric_muganga.cinema.admin.service.IAdminReservationCommandService;
import dev.eric_muganga.cinema.movie.dto.MovieWithShowtimesDto;
import dev.eric_muganga.cinema.movie.service.MovieBrowseService;
import dev.eric_muganga.cinema.reservation.dto.AdminReservationView;
import dev.eric_muganga.cinema.reservation.dto.PagedShowtimeReportResponse;
import dev.eric_muganga.cinema.reservation.dto.ShowtimeReport;
import dev.eric_muganga.cinema.admin.service.IAdminReportService;
import dev.eric_muganga.cinema.admin.service.IAdminReservationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {
    private final IAdminReportService adminReportService;
    private final MovieBrowseService movieBrowseService;
    private final IAdminReservationQueryService adminReservationQueryService;
    private final IAdminReservationCommandService adminReservationCommandService;

    @GetMapping("/admin/showtimes/daily")
    public String dailyShowtimePerformance(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            Model model
    ) {
        LocalDate businessDate = (date != null) ? date : LocalDate.now();

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PagedShowtimeReportResponse reports =
                adminReportService.getShowtimeReportsForDate(businessDate, pageable);

        model.addAttribute("reports", reports);
        model.addAttribute("businessDate", businessDate);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("title", "Daily Showtime Performance");

        return "admin/daily-showtimes";
    }

    @GetMapping("/admin/movies")
    public String movies(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model
    ) {
        LocalDate businessDate = (date != null) ? date : LocalDate.now();

        List<MovieWithShowtimesDto> movies = movieBrowseService.getMoviesByDate(businessDate);

        int totalShowtimes = movies.stream()
                .mapToInt(movie -> movie.showtimes().size())
                .sum();

        model.addAttribute("movies", movies);
        model.addAttribute("businessDate", businessDate);
        model.addAttribute("totalShowtimes", totalShowtimes);
        model.addAttribute("title", "Movies");

        return "admin/movies";
    }

    @GetMapping("/admin/revenue")
    public String revenue(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        LocalDate businessDate = (date != null) ? date : LocalDate.now();

        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());

        PagedShowtimeReportResponse reports =
                adminReportService.getShowtimeReportsForDate(businessDate, pageable);

        BigDecimal totalRevenue = reports.items().stream()
                .map(report -> report.totalRevenue() == null ? BigDecimal.ZERO : report.totalRevenue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalReservedSeats = reports.items().stream()
                .mapToInt(ShowtimeReport::reservedSeats)
                .sum();

        model.addAttribute("reports", reports);
        model.addAttribute("businessDate", businessDate);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalReservedSeats", totalReservedSeats);
        model.addAttribute("title", "Revenue");

        return "admin/revenue";
    }

    @GetMapping("/admin/reservations")
    public String reservations(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<AdminReservationView> reservations =
                adminReservationQueryService.getReservations(status, pageable);

        model.addAttribute("reservations", reservations);
        model.addAttribute("status", status);
        model.addAttribute("size", size);
        model.addAttribute("title", "Reservations");

        return "admin/reservations";
    }

    @GetMapping("/admin/reservations/{reservationId}")
    public String reservationDetail(@PathVariable Long reservationId, Model model) {
        model.addAttribute("reservation", adminReservationQueryService.getReservationDetail(reservationId));
        model.addAttribute("title", "Reservation Detail");
        return "admin/reservation-detail";
    }

    @PostMapping("/admin/reservations/{reservationId}/cancel")
    public String cancelReservation(
            @PathVariable Long reservationId,
            RedirectAttributes redirectAttributes
    ) {
        var result = adminReservationCommandService.cancelReservation(reservationId);
        redirectAttributes.addFlashAttribute("successMessage", result.message());
        return "redirect:/admin/reservations/" + reservationId;
    }
}
