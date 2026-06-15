package dev.eric_muganga.cinema.movie.service;

import dev.eric_muganga.cinema.movie.dto.MovieWithShowtimesDto;
import dev.eric_muganga.cinema.movie.dto.ShowtimeDto;
import dev.eric_muganga.cinema.movie.entity.Movie;
import dev.eric_muganga.cinema.movie.repository.MovieRepository;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class MovieBrowseService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public List<MovieWithShowtimesDto> getMoviesByDate(LocalDate date) {
        OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = from.plusDays(1);

        List<Showtime> showtimes = showtimeRepository.findByStartTimeBetween(from, to);

        // Group showtimes by movie id
        Map<Long, List<Showtime>> showtimesByMovieId = showtimes.stream()
                .collect(Collectors.groupingBy(st -> st.getMovie().getId()));

        if (showtimesByMovieId.isEmpty()) {
            return List.of();
        }

        // Load movies in one query
        List<Long> movieIds = showtimesByMovieId.keySet().stream().toList();
        Map<Long, Movie> moviesById = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, m -> m));

        // Map to DTOs
        return showtimesByMovieId.entrySet().stream()
                .map(entry -> {
                    Long movieId = entry.getKey();
                    Movie movie = moviesById.get(movieId);

                    List<ShowtimeDto> showtimeDtos = entry.getValue().stream()
                            .map(this::toShowtimeDto)
                            .sorted((a, b) -> a.startTime().compareTo(b.startTime()))
                            .toList();

                    return new MovieWithShowtimesDto(
                            movie.getId(),
                            movie.getTitle(),
                            movie.getPosterUrl(),
                            showtimeDtos
                    );
                })
                .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
                .toList();
    }

    private ShowtimeDto toShowtimeDto(Showtime showtime) {
        return new ShowtimeDto(
                showtime.getId(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice()
        );
    }
}
