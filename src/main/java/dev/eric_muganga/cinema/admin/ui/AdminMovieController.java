package dev.eric_muganga.cinema.admin.ui;

import dev.eric_muganga.cinema.movie.dto.AdminMovieRequest;
import dev.eric_muganga.cinema.movie.dto.AdminMovieView;
import dev.eric_muganga.cinema.movie.entity.Genre;
import dev.eric_muganga.cinema.movie.repository.GenreRepository;
import dev.eric_muganga.cinema.movie.service.IAdminMovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/movies")
public class AdminMovieController {

    private final IAdminMovieService adminMovieService;
    private final GenreRepository genreRepository;

    @GetMapping
    public String movies(Model model) {
        model.addAttribute("movies", adminMovieService.getAllMovies());
        model.addAttribute("title", "Movies");
        return "admin/movies";
    }

    @GetMapping("/new")
    public String newMovie(Model model) {
        model.addAttribute("movieForm", new AdminMovieRequest("", "", "", 120, List.of()));
        model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());
        model.addAttribute("title", "Create Movie");
        return "admin/movie-form";
    }

    @PostMapping
    public String createMovie(
            @Valid @ModelAttribute("movieForm") AdminMovieRequest movieForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());
            model.addAttribute("title", "Create Movie");
            return "admin/movie-form";
        }

        AdminMovieView movie = adminMovieService.createMovie(movieForm);
        redirectAttributes.addFlashAttribute("successMessage", "Movie created successfully.");
        return "redirect:/admin/movies/" + movie.id() + "/edit";
    }

    @GetMapping("/{movieId}/edit")
    public String editMovie(@PathVariable Long movieId, Model model) {
        AdminMovieView movie = adminMovieService.getMovie(movieId);

        model.addAttribute("movieId", movieId);
        model.addAttribute("movieForm", new AdminMovieRequest(
                movie.title(),
                movie.description(),
                movie.posterUrl(),
                movie.durationMinutes(),
                genreRepository.findAllByOrderByNameAsc().stream()
                        .filter(g -> movie.genres().contains(g.getName()))
                        .map(Genre::getId)
                        .toList()
        ));
        model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());
        model.addAttribute("title", "Edit Movie");
        return "admin/movie-form";
    }

    @PostMapping("/{movieId}")
    public String updateMovie(
            @PathVariable Long movieId,
            @Valid @ModelAttribute("movieForm") AdminMovieRequest movieForm,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("movieId", movieId);
            model.addAttribute("genres", genreRepository.findAllByOrderByNameAsc());
            model.addAttribute("title", "Edit Movie");
            return "admin/movie-form";
        }

        adminMovieService.updateMovie(movieId, movieForm);
        redirectAttributes.addFlashAttribute("successMessage", "Movie updated successfully.");
        return "redirect:/admin/movies";
    }

    @PostMapping("/{movieId}/delete")
    public String deleteMovie(@PathVariable Long movieId, RedirectAttributes redirectAttributes) {
        adminMovieService.deleteMovie(movieId);
        redirectAttributes.addFlashAttribute("successMessage", "Movie deleted successfully.");
        return "redirect:/admin/movies";
    }
}