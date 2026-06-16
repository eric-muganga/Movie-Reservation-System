package dev.eric_muganga.cinema.movie.dto;

import java.util.List;

public record MovieWithShowtimesDto(
        Long movieId,
        String title,
        String posterUrl,
        List<ShowtimeDto> showtimes
) {}
