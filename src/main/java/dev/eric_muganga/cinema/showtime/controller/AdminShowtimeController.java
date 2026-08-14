package dev.eric_muganga.cinema.showtime.controller;

import dev.eric_muganga.cinema.movie.repository.MovieRepository;
import dev.eric_muganga.cinema.showtime.dto.AdminShowtimeRequest;
import dev.eric_muganga.cinema.showtime.dto.AdminShowtimeView;
import dev.eric_muganga.cinema.showtime.service.IAdminShowtimeService;
import dev.eric_muganga.cinema.venue.repository.AuditoriumRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/showtimes")
public class AdminShowtimeController {

    private final IAdminShowtimeService adminShowtimeService;
    private final MovieRepository movieRepository;
    private final AuditoriumRepository auditoriumRepository;

    @GetMapping
    public String showtimes(Model model) {
        var showtimesV = adminShowtimeService.getAllShowtimes();

        long scheduledMovies = showtimesV.stream()
                .map(AdminShowtimeView::movieId)
                .distinct()
                .count();

        long auditoriumsInUse = showtimesV.stream()
                .map(AdminShowtimeView::auditoriumId)
                .distinct()
                .count();

        model.addAttribute("showtimes", showtimesV);
        model.addAttribute("totalShowtimes", showtimesV.size());
        model.addAttribute("scheduledMovies", scheduledMovies);
        model.addAttribute("auditoriumsInUse", auditoriumsInUse);
        model.addAttribute("title", "Showtimes");

        return "admin/showtimes";
    }

    @GetMapping("/new")
    public String newShowtime(Model model) {
        model.addAttribute(
                "showtimeForm",
                new AdminShowtimeRequest(null, null, null, null, BigDecimal.ZERO)
        );
        populateFormOptions(model);
        model.addAttribute("title", "Schedule Showtime");
        return "admin/showtime-form";
    }

    @PostMapping
    public String createShowtime(
            @Valid @ModelAttribute("showtimeForm") AdminShowtimeRequest showtimeForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(model);
            model.addAttribute("title", "Schedule Showtime");
            return "admin/showtime-form";
        }

        adminShowtimeService.createShowtime(showtimeForm);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Showtime scheduled successfully."
        );

        return "redirect:/admin/showtimes";
    }

    @GetMapping("/{showtimeId}/edit")
    public String editShowtime(
            @PathVariable Long showtimeId,
            Model model
    ) {
        AdminShowtimeView showtime = adminShowtimeService.getShowtime(showtimeId);

        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute(
                "showtimeForm",
                new AdminShowtimeRequest(
                        showtime.movieId(),
                        showtime.auditoriumId(),
                        showtime.startTime().toLocalDateTime(),
                        showtime.endTime().toLocalDateTime(),
                        showtime.basePrice()
                )
        );

        populateFormOptions(model);
        model.addAttribute("title", "Edit Showtime");

        return "admin/showtime-form";
    }

    @PostMapping("/{showtimeId}")
    public String updateShowtime(
            @PathVariable Long showtimeId,
            @Valid @ModelAttribute("showtimeForm") AdminShowtimeRequest showtimeForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("showtimeId", showtimeId);
            populateFormOptions(model);
            model.addAttribute("title", "Edit Showtime");
            return "admin/showtime-form";
        }

        adminShowtimeService.updateShowtime(showtimeId, showtimeForm);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Showtime updated successfully."
        );

        return "redirect:/admin/showtimes";
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("auditoriums", auditoriumRepository.findAll());
    }
}