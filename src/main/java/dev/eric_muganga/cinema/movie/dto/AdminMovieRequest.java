package dev.eric_muganga.cinema.movie.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminMovieRequest(
        @NotBlank String title,
        @NotBlank String description,
        String posterUrl,
        @NotNull @Min(1) Integer durationMinutes,
        List<Long> genreIds
) {
}
