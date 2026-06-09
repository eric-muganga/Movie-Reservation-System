package dev.eric_muganga.cinema.movie.repository;

import dev.eric_muganga.cinema.movie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    // Simple search by title, case-insensitive
    List<Movie> findByTitleContainingIgnoreCase(String title);

    // Paged version for UI lists
    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    // Filter by a single genre name
    List<Movie> findByGenres_NameIgnoreCase(String genreName);

    Page<Movie> findByGenres_NameIgnoreCase(String genreName, Pageable pageable);

    // Filter by title + genre together
    Page<Movie> findByTitleContainingIgnoreCaseAndGenres_NameIgnoreCase(
            String titlePart,
            String genreName,
            Pageable pageable
    );
}