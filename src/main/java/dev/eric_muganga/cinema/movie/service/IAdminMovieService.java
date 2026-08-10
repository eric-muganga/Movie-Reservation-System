package dev.eric_muganga.cinema.movie.service;

import dev.eric_muganga.cinema.movie.dto.AdminMovieRequest;
import dev.eric_muganga.cinema.movie.dto.AdminMovieView;

import java.util.List;

public interface IAdminMovieService {
    List<AdminMovieView> getAllMovies();
    AdminMovieView getMovie(Long movieId);
    AdminMovieView createMovie(AdminMovieRequest request);
    AdminMovieView updateMovie(Long movieId, AdminMovieRequest request);
    void deleteMovie(Long movieId);
}
