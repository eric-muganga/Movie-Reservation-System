package dev.eric_muganga.cinema.movie;


import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.movie.dto.AdminMovieRequest;
import dev.eric_muganga.cinema.movie.dto.AdminMovieView;
import dev.eric_muganga.cinema.movie.entity.Genre;
import dev.eric_muganga.cinema.movie.entity.Movie;
import dev.eric_muganga.cinema.movie.repository.GenreRepository;
import dev.eric_muganga.cinema.movie.repository.MovieRepository;
import dev.eric_muganga.cinema.movie.service.AdminMovieServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private AdminMovieServiceImpl adminMovieService;

    @Test
    void createMovie_savesMovieWithSelectedGenres() {
        Genre action = Genre.builder()
                .id(1L)
                .name("Action")
                .build();

        Genre sciFi = Genre.builder()
                .id(2L)
                .name("Sci-Fi")
                .build();

        AdminMovieRequest request = new AdminMovieRequest(
                "The Matrix",
                "A hacker discovers that reality is a simulation.",
                "https://example.com/matrix.jpg",
                136,
                List.of(1L, 2L)
        );

        when(genreRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(action, sciFi));

        when(movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> {
                    Movie movie = invocation.getArgument(0);
                    movie.setId(10L);
                    return movie;
                });

        AdminMovieView result = adminMovieService.createMovie(request);

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());

        Movie savedMovie = movieCaptor.getValue();

        assertThat(savedMovie.getId()).isEqualTo(10L);
        assertThat(savedMovie.getTitle()).isEqualTo("The Matrix");
        assertThat(savedMovie.getDescription())
                .isEqualTo("A hacker discovers that reality is a simulation.");
        assertThat(savedMovie.getPosterUrl()).isEqualTo("https://example.com/matrix.jpg");
        assertThat(savedMovie.getDurationMinutes()).isEqualTo(136);
        assertThat(savedMovie.getGenres())
                .extracting(Genre::getName)
                .containsExactlyInAnyOrder("Action", "Sci-Fi");

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.title()).isEqualTo("The Matrix");
        assertThat(result.durationMinutes()).isEqualTo(136);
        assertThat(result.genres()).containsExactly("Action", "Sci-Fi");

        verify(genreRepository).findAllById(List.of(1L, 2L));
    }

    @Test
    void createMovie_withoutGenres_savesMovieWithEmptyGenreList() {
        AdminMovieRequest request = new AdminMovieRequest(
                "Solo Movie",
                "A movie with no selected genres.",
                null,
                95,
                List.of()
        );

        when(movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> {
                    Movie movie = invocation.getArgument(0);
                    movie.setId(11L);
                    return movie;
                });

        AdminMovieView result = adminMovieService.createMovie(request);

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.genres()).isEmpty();

        verify(movieRepository).save(argThat(movie ->
                movie.getGenres().isEmpty()
                        && movie.getTitle().equals("Solo Movie")
        ));
        verifyNoInteractions(genreRepository);
    }

    @Test
    void createMovie_throwsWhenOneOrMoreGenresDoNotExist() {
        Genre action = Genre.builder()
                .id(1L)
                .name("Action")
                .build();

        AdminMovieRequest request = new AdminMovieRequest(
                "Missing Genre Movie",
                "This request contains an invalid genre.",
                null,
                100,
                List.of(1L, 999L)
        );

        when(genreRepository.findAllById(List.of(1L, 999L)))
                .thenReturn(List.of(action));

        assertThatThrownBy(() -> adminMovieService.createMovie(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("One or more genres not found");

        verify(movieRepository, never()).save(any());
    }

    @Test
    void updateMovie_updatesExistingMovieInsteadOfCreatingNewMovie() {
        Genre drama = Genre.builder()
                .id(3L)
                .name("Drama")
                .build();

        Genre thriller = Genre.builder()
                .id(4L)
                .name("Thriller")
                .build();

        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(2);

        Movie existingMovie = Movie.builder()
                .id(20L)
                .title("Inception")
                .description("Original description")
                .posterUrl("https://example.com/original.jpg")
                .durationMinutes(148)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .genres(Set.of(drama))
                .build();

        AdminMovieRequest request = new AdminMovieRequest(
                "Inception Updated",
                "Updated description.",
                "https://example.com/inception-updated.jpg",
                150,
                List.of(3L, 4L)
        );

        when(movieRepository.findById(20L)).thenReturn(Optional.of(existingMovie));
        when(genreRepository.findAllById(List.of(3L, 4L)))
                .thenReturn(List.of(drama, thriller));
        when(movieRepository.save(existingMovie)).thenReturn(existingMovie);

        AdminMovieView result = adminMovieService.updateMovie(20L, request);

        assertThat(existingMovie.getId()).isEqualTo(20L);
        assertThat(existingMovie.getTitle()).isEqualTo("Inception Updated");
        assertThat(existingMovie.getDescription()).isEqualTo("Updated description.");
        assertThat(existingMovie.getPosterUrl())
                .isEqualTo("https://example.com/inception-updated.jpg");
        assertThat(existingMovie.getDurationMinutes()).isEqualTo(150);
        assertThat(existingMovie.getCreatedAt()).isEqualTo(createdAt);
        assertThat(existingMovie.getUpdatedAt()).isAfter(createdAt);
        assertThat(existingMovie.getGenres())
                .extracting(Genre::getName)
                .containsExactlyInAnyOrder("Drama", "Thriller");

        assertThat(result.id()).isEqualTo(20L);
        assertThat(result.title()).isEqualTo("Inception Updated");
        assertThat(result.genres()).containsExactly("Drama", "Thriller");

        verify(movieRepository).findById(20L);
        verify(movieRepository).save(existingMovie);
        verify(movieRepository, never()).save(argThat(movie ->
                movie != existingMovie && movie.getTitle().equals("Inception Updated")
        ));
    }

    @Test
    void updateMovie_throwsWhenMovieDoesNotExist() {
        AdminMovieRequest request = new AdminMovieRequest(
                "Unknown Movie",
                "Description",
                null,
                100,
                List.of()
        );

        when(movieRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminMovieService.updateMovie(404L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Movie not found: 404");

        verify(movieRepository, never()).save(any());
        verifyNoInteractions(genreRepository);
    }

    @Test
    void getAllMovies_returnsAdminViewsWithSortedGenreNames() {
        Genre thriller = Genre.builder().id(1L).name("Thriller").build();
        Genre action = Genre.builder().id(2L).name("Action").build();

        OffsetDateTime now = OffsetDateTime.now();

        Movie movie = Movie.builder()
                .id(30L)
                .title("John Wick")
                .description("An ex-hitman seeks revenge.")
                .posterUrl("https://example.com/john-wick.jpg")
                .durationMinutes(101)
                .createdAt(now)
                .updatedAt(now)
                .genres(Set.of(thriller, action))
                .build();

        when(movieRepository.findAll()).thenReturn(List.of(movie));

        List<AdminMovieView> results = adminMovieService.getAllMovies();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().id()).isEqualTo(30L);
        assertThat(results.getFirst().title()).isEqualTo("John Wick");
        assertThat(results.getFirst().genres()).containsExactly("Action", "Thriller");
    }

    @Test
    void deleteMovie_deletesWhenMovieExists() {
        when(movieRepository.existsById(50L)).thenReturn(true);

        adminMovieService.deleteMovie(50L);

        verify(movieRepository).deleteById(50L);
    }

    @Test
    void deleteMovie_throwsWhenMovieDoesNotExist() {
        when(movieRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> adminMovieService.deleteMovie(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Movie not found: 404");

        verify(movieRepository, never()).deleteById(anyLong());
    }
}