package dev.eric_muganga.cinema.showtime.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.ShowtimeConflictException;
import dev.eric_muganga.cinema.movie.entity.Movie;
import dev.eric_muganga.cinema.movie.repository.MovieRepository;
import dev.eric_muganga.cinema.showtime.dto.AdminShowtimeRequest;
import dev.eric_muganga.cinema.showtime.dto.AdminShowtimeView;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.showtime.entity.ShowtimeStatus;
import dev.eric_muganga.cinema.showtime.repository.ShowtimeRepository;
import dev.eric_muganga.cinema.venue.entity.Auditorium;
import dev.eric_muganga.cinema.venue.repository.AuditoriumRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminShowtimeServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private AuditoriumRepository auditoriumRepository;

    @InjectMocks
    private AdminShowtimeServiceImpl adminShowtimeService;

    @Test
    void getAllShowtimes_returnsShowtimesOrderedByStartTime() {
        Movie movie = movie(10L, "John Wick");
        Auditorium auditorium = auditorium(20L, "Hall 1");

        Showtime first = showtime(
                1L,
                movie,
                auditorium,
                OffsetDateTime.of(2026, 8, 15, 14, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 15, 16, 0, 0, 0, ZoneOffset.UTC),
                ShowtimeStatus.SCHEDULED,
                new BigDecimal("10.00")
        );

        Showtime second = showtime(
                2L,
                movie,
                auditorium,
                OffsetDateTime.of(2026, 8, 15, 18, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 15, 20, 0, 0, 0, ZoneOffset.UTC),
                ShowtimeStatus.OPEN,
                new BigDecimal("12.50")
        );

        when(showtimeRepository.findAllByOrderByStartTimeAsc())
                .thenReturn(List.of(first, second));

        List<AdminShowtimeView> results = adminShowtimeService.getAllShowtimes();

        assertThat(results).hasSize(2);
        assertThat(results.getFirst().id()).isEqualTo(1L);
        assertThat(results.getFirst().movieTitle()).isEqualTo("John Wick");
        assertThat(results.getFirst().auditoriumName()).isEqualTo("Hall 1");
        assertThat(results.getFirst().status()).isEqualTo(ShowtimeStatus.SCHEDULED);

        assertThat(results.get(1).id()).isEqualTo(2L);
        assertThat(results.get(1).basePrice()).isEqualByComparingTo("12.50");

        verify(showtimeRepository).findAllByOrderByStartTimeAsc();
    }

    @Test
    void getShowtime_returnsMappedViewWhenShowtimeExists() {
        Movie movie = movie(10L, "Dune");
        Auditorium auditorium = auditorium(20L, "Hall 2");

        Showtime showtime = showtime(
                30L,
                movie,
                auditorium,
                OffsetDateTime.of(2026, 8, 15, 19, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 15, 22, 0, 0, 0, ZoneOffset.UTC),
                ShowtimeStatus.OPEN,
                new BigDecimal("15.00")
        );

        when(showtimeRepository.findById(30L)).thenReturn(Optional.of(showtime));

        AdminShowtimeView result = adminShowtimeService.getShowtime(30L);

        assertThat(result.id()).isEqualTo(30L);
        assertThat(result.movieId()).isEqualTo(10L);
        assertThat(result.movieTitle()).isEqualTo("Dune");
        assertThat(result.auditoriumId()).isEqualTo(20L);
        assertThat(result.auditoriumName()).isEqualTo("Hall 2");
        assertThat(result.status()).isEqualTo(ShowtimeStatus.OPEN);
        assertThat(result.basePrice()).isEqualByComparingTo("15.00");
    }

    @Test
    void getShowtime_throwsWhenShowtimeDoesNotExist() {
        when(showtimeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminShowtimeService.getShowtime(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Showtime not found: 404");
    }

    @Test
    void createShowtime_createsScheduledShowtimeWhenRequestIsValid() {
        Movie movie = movie(10L, "Interstellar");
        Auditorium auditorium = auditorium(20L, "Hall 1");

        AdminShowtimeRequest request = request(
                10L,
                20L,
                LocalDateTime.of(2026, 8, 20, 18, 0),
                LocalDateTime.of(2026, 8, 20, 21, 0),
                new BigDecimal("14.50")
        );

        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(auditoriumRepository.findById(20L)).thenReturn(Optional.of(auditorium));

        when(showtimeRepository
                .existsByAuditoriumIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        eq(20L),
                        any(OffsetDateTime.class),
                        any(OffsetDateTime.class)
                ))
                .thenReturn(false);

        when(showtimeRepository.save(any(Showtime.class)))
                .thenAnswer(invocation -> {
                    Showtime savedShowtime = invocation.getArgument(0);
                    savedShowtime.setId(100L);
                    return savedShowtime;
                });

        AdminShowtimeView result = adminShowtimeService.createShowtime(request);

        ArgumentCaptor<Showtime> showtimeCaptor = ArgumentCaptor.forClass(Showtime.class);

        verify(showtimeRepository).save(showtimeCaptor.capture());

        Showtime savedShowtime = showtimeCaptor.getValue();

        assertThat(savedShowtime.getMovie()).isSameAs(movie);
        assertThat(savedShowtime.getAuditorium()).isSameAs(auditorium);
        assertThat(savedShowtime.getStartTime())
                .isEqualTo(OffsetDateTime.of(2026, 8, 20, 18, 0, 0, 0, ZoneOffset.UTC));
        assertThat(savedShowtime.getEndTime())
                .isEqualTo(OffsetDateTime.of(2026, 8, 20, 21, 0, 0, 0, ZoneOffset.UTC));
        assertThat(savedShowtime.getBasePrice()).isEqualByComparingTo("14.50");
        assertThat(savedShowtime.getStatus()).isEqualTo(ShowtimeStatus.SCHEDULED);

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.movieTitle()).isEqualTo("Interstellar");
        assertThat(result.auditoriumName()).isEqualTo("Hall 1");
        assertThat(result.status()).isEqualTo(ShowtimeStatus.SCHEDULED);
    }

    @Test
    void createShowtime_throwsWhenMovieDoesNotExist() {
        AdminShowtimeRequest request = request(
                404L,
                20L,
                LocalDateTime.of(2026, 8, 20, 18, 0),
                LocalDateTime.of(2026, 8, 20, 20, 0),
                new BigDecimal("10.00")
        );

        when(movieRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminShowtimeService.createShowtime(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Movie not found: 404");

        verifyNoInteractions(auditoriumRepository);
        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void createShowtime_throwsWhenAuditoriumDoesNotExist() {
        Movie movie = movie(10L, "The Matrix");

        AdminShowtimeRequest request = request(
                10L,
                404L,
                LocalDateTime.of(2026, 8, 20, 18, 0),
                LocalDateTime.of(2026, 8, 20, 20, 0),
                new BigDecimal("10.00")
        );

        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(auditoriumRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminShowtimeService.createShowtime(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Auditorium not found: 404");

        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void createShowtime_throwsWhenAuditoriumHasOverlappingShowtime() {
        Movie movie = movie(10L, "Oppenheimer");
        Auditorium auditorium = auditorium(20L, "Hall 3");

        AdminShowtimeRequest request = request(
                10L,
                20L,
                LocalDateTime.of(2026, 8, 20, 18, 0),
                LocalDateTime.of(2026, 8, 20, 21, 0),
                new BigDecimal("11.00")
        );

        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(auditoriumRepository.findById(20L)).thenReturn(Optional.of(auditorium));

        when(showtimeRepository
                .existsByAuditoriumIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        20L,
                        OffsetDateTime.of(2026, 8, 20, 21, 0, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2026, 8, 20, 18, 0, 0, 0, ZoneOffset.UTC)
                ))
                .thenReturn(true);

        assertThatThrownBy(() -> adminShowtimeService.createShowtime(request))
                .isInstanceOf(ShowtimeConflictException.class)
                .hasMessage(
                        "This auditorium already has a showtime during the selected time range."
                );

        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void updateShowtime_updatesExistingShowtimeWhenNoOtherShowtimeConflicts() {
        Movie existingMovie = movie(10L, "Old Movie");
        Movie updatedMovie = movie(11L, "New Movie");

        Auditorium existingAuditorium = auditorium(20L, "Hall 1");
        Auditorium updatedAuditorium = auditorium(21L, "Hall 2");

        Showtime existingShowtime = showtime(
                30L,
                existingMovie,
                existingAuditorium,
                OffsetDateTime.of(2026, 8, 20, 15, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 20, 17, 0, 0, 0, ZoneOffset.UTC),
                ShowtimeStatus.OPEN,
                new BigDecimal("10.00")
        );

        AdminShowtimeRequest request = request(
                11L,
                21L,
                LocalDateTime.of(2026, 8, 20, 19, 0),
                LocalDateTime.of(2026, 8, 20, 22, 0),
                new BigDecimal("16.50")
        );

        when(showtimeRepository.findById(30L)).thenReturn(Optional.of(existingShowtime));
        when(movieRepository.findById(11L)).thenReturn(Optional.of(updatedMovie));
        when(auditoriumRepository.findById(21L)).thenReturn(Optional.of(updatedAuditorium));

        when(showtimeRepository
                .existsByAuditoriumIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        eq(21L),
                        eq(30L),
                        any(OffsetDateTime.class),
                        any(OffsetDateTime.class)
                ))
                .thenReturn(false);

        when(showtimeRepository.save(existingShowtime)).thenReturn(existingShowtime);

        AdminShowtimeView result = adminShowtimeService.updateShowtime(30L, request);

        assertThat(existingShowtime.getMovie()).isSameAs(updatedMovie);
        assertThat(existingShowtime.getAuditorium()).isSameAs(updatedAuditorium);
        assertThat(existingShowtime.getStartTime())
                .isEqualTo(OffsetDateTime.of(2026, 8, 20, 19, 0, 0, 0, ZoneOffset.UTC));
        assertThat(existingShowtime.getEndTime())
                .isEqualTo(OffsetDateTime.of(2026, 8, 20, 22, 0, 0, 0, ZoneOffset.UTC));
        assertThat(existingShowtime.getBasePrice()).isEqualByComparingTo("16.50");

        // Editing schedule details must not silently change the current status.
        assertThat(existingShowtime.getStatus()).isEqualTo(ShowtimeStatus.OPEN);

        assertThat(result.movieTitle()).isEqualTo("New Movie");
        assertThat(result.auditoriumName()).isEqualTo("Hall 2");
        assertThat(result.status()).isEqualTo(ShowtimeStatus.OPEN);

        verify(showtimeRepository).save(existingShowtime);
    }

    @Test
    void updateShowtime_throwsWhenAnotherShowtimeOverlapsInAuditorium() {
        Movie movie = movie(10L, "Avatar");
        Auditorium auditorium = auditorium(20L, "Hall 1");

        Showtime existingShowtime = showtime(
                30L,
                movie,
                auditorium,
                OffsetDateTime.of(2026, 8, 20, 12, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 8, 20, 14, 0, 0, 0, ZoneOffset.UTC),
                ShowtimeStatus.SCHEDULED,
                new BigDecimal("10.00")
        );

        AdminShowtimeRequest request = request(
                10L,
                20L,
                LocalDateTime.of(2026, 8, 20, 18, 0),
                LocalDateTime.of(2026, 8, 20, 20, 0),
                new BigDecimal("12.00")
        );

        when(showtimeRepository.findById(30L)).thenReturn(Optional.of(existingShowtime));
        when(movieRepository.findById(10L)).thenReturn(Optional.of(movie));
        when(auditoriumRepository.findById(20L)).thenReturn(Optional.of(auditorium));

        when(showtimeRepository
                .existsByAuditoriumIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                        20L,
                        30L,
                        OffsetDateTime.of(2026, 8, 20, 20, 0, 0, 0, ZoneOffset.UTC),
                        OffsetDateTime.of(2026, 8, 20, 18, 0, 0, 0, ZoneOffset.UTC)
                ))
                .thenReturn(true);

        assertThatThrownBy(() -> adminShowtimeService.updateShowtime(30L, request))
                .isInstanceOf(ShowtimeConflictException.class)
                .hasMessage(
                        "This auditorium already has a showtime during the selected time range."
                );

        verify(showtimeRepository, never()).save(any());
    }

    @Test
    void updateShowtime_throwsWhenShowtimeDoesNotExist() {
        AdminShowtimeRequest request = request(
                10L,
                20L,
                LocalDateTime.of(2026, 8, 20, 18, 0),
                LocalDateTime.of(2026, 8, 20, 20, 0),
                new BigDecimal("10.00")
        );

        when(showtimeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminShowtimeService.updateShowtime(404L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Showtime not found: 404");

        verifyNoInteractions(movieRepository, auditoriumRepository);
        verify(showtimeRepository, never()).save(any());
    }

    private AdminShowtimeRequest request(
            Long movieId,
            Long auditoriumId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal basePrice
    ) {
        return new AdminShowtimeRequest(
                movieId,
                auditoriumId,
                startTime,
                endTime,
                basePrice
        );
    }

    private Movie movie(Long id, String title) {
        return Movie.builder()
                .id(id)
                .title(title)
                .description("Test movie description")
                .durationMinutes(120)
                .build();
    }

    private Auditorium auditorium(Long id, String name) {
        return Auditorium.builder()
                .id(id)
                .name(name)
                .totalRows(10)
                .totalCols(12)
                .build();
    }

    private Showtime showtime(
            Long id,
            Movie movie,
            Auditorium auditorium,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            ShowtimeStatus status,
            BigDecimal basePrice
    ) {
        return Showtime.builder()
                .id(id)
                .movie(movie)
                .auditorium(auditorium)
                .startTime(startTime)
                .endTime(endTime)
                .status(status)
                .basePrice(basePrice)
                .build();
    }
}