package dev.eric_muganga.cinema.admin.ui;

import dev.eric_muganga.cinema.reservation.dto.PagedShowtimeReportResponse;
import dev.eric_muganga.cinema.reservation.service.IAdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {
    private final IAdminReportService adminReportService;

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
        model.addAttribute("content", "admin/daily-showtimes :: content");
        model.addAttribute("title", "Daily Showtime Performance");

        return "admin/layout";
    }
}
