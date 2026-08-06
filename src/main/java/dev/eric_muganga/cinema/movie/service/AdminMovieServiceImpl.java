package dev.eric_muganga.cinema.movie.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.movie.dto.AdminMovieRequest;
import dev.eric_muganga.cinema.movie.dto.AdminMovieView;
import dev.eric_muganga.cinema.movie.entity.Genre;
import dev.eric_muganga.cinema.movie.entity.Movie;
import dev.eric_muganga.cinema.movie.repository.GenreRepository;
import dev.eric_muganga.cinema.movie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminMovieServiceImpl implements IAdminMovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminMovieView> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMovieView getMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));
        return toView(movie);
    }

    @Override
    public AdminMovieView createMovie(AdminMovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setPosterUrl(request.posterUrl());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setCreatedAt(OffsetDateTime.now());
        movie.setUpdatedAt(OffsetDateTime.now());
        movie.setGenres(resolveGenres(request.genreIds()));

        return toView(movieRepository.save(movie));
    }

    @Override
    public AdminMovieView updateMovie(Long movieId, AdminMovieRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found: " + movieId));

        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setPosterUrl(request.posterUrl());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setUpdatedAt(OffsetDateTime.now());
        movie.setGenres(resolveGenres(request.genreIds()));

        return toView(movieRepository.save(movie));
    }

    @Override
    public void deleteMovie(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Movie not found: " + movieId);
        }
        movieRepository.deleteById(movieId);
    }

    private Set<Genre> resolveGenres(List<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Genre> genres = genreRepository.findAllById(genreIds);
        if (genres.size() != genreIds.size()) {
            throw new ResourceNotFoundException("One or more genres not found");
        }
        return new HashSet<>(genres);
    }

    private AdminMovieView toView(Movie movie) {
        List<String> genreNames = movie.getGenres().stream()
                .map(Genre::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new AdminMovieView(
                movie.getId(),
                movie.getTitle(),
                movie.getDescription(),
                movie.getPosterUrl(),
                movie.getDurationMinutes(),
                genreNames,
                movie.getCreatedAt(),
                movie.getUpdatedAt()
        );
    }
}
