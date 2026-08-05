package dev.eric_muganga.cinema.movie.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminMovieView (
        Long id,
        String title,
        String description,
        String posterUrl,
        Integer durationMinutes,
        List<String> genres,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
