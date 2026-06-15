package dev.eric_muganga.cinema.movie.controller;

import dev.eric_muganga.cinema.movie.dto.MovieWithShowtimesDto;
import dev.eric_muganga.cinema.movie.service.MovieBrowseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/movies")
public class MovieBrowseController {
    private final MovieBrowseService movieBrowseService;

    @GetMapping
    public List<MovieWithShowtimesDto> getMoviesByDate(
            @RequestParam("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return movieBrowseService.getMoviesByDate(date);
    }

}
