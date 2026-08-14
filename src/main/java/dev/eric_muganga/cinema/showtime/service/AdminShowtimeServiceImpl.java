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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminShowtimeServiceImpl implements IAdminShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final AuditoriumRepository auditoriumRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AdminShowtimeView> getAllShowtimes() {
        return showtimeRepository.findAllByOrderByStartTimeAsc().stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminShowtimeView getShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Showtime not found: " + showtimeId)
                );

        return toView(showtime);
    }

    @Override
    public AdminShowtimeView createShowtime(AdminShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found: " + request.movieId())
                );

        Auditorium auditorium = auditoriumRepository.findById(request.auditoriumId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Auditorium not found: " + request.auditoriumId()
                        )
                );

        var startTime = request.startTime().atOffset(ZoneOffset.UTC);
        var endTime = request.endTime().atOffset(ZoneOffset.UTC);

        boolean hasConflict =
                showtimeRepository.existsByAuditoriumIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        auditorium.getId(),
                        endTime,
                        startTime
                );

        if (hasConflict) {
            throw new ShowtimeConflictException(
                    "This auditorium already has a showtime during the selected time range."
            );
        }

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .startTime(startTime)
                .endTime(endTime)
                .basePrice(request.basePrice())
                .status(ShowtimeStatus.SCHEDULED)
                .build();

        return toView(showtimeRepository.save(showtime));
    }

    @Override
    public AdminShowtimeView updateShowtime(
            Long showtimeId,
            AdminShowtimeRequest request
    ) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Showtime not found: " + showtimeId)
                );

        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found: " + request.movieId())
                );

        Auditorium auditorium = auditoriumRepository.findById(request.auditoriumId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Auditorium not found: " + request.auditoriumId()
                        )
                );

        var startTime = request.startTime().atOffset(ZoneOffset.UTC);
        var endTime = request.endTime().atOffset(ZoneOffset.UTC);

        boolean hasConflict =
                showtimeRepository
                        .existsByAuditoriumIdAndIdNotAndStartTimeLessThanAndEndTimeGreaterThan(
                                auditorium.getId(),
                                showtimeId,
                                endTime,
                                startTime
                        );

        if (hasConflict) {
            throw new ShowtimeConflictException(
                    "This auditorium already has a showtime during the selected time range."
            );
        }

        showtime.setMovie(movie);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(startTime);
        showtime.setEndTime(endTime);
        showtime.setBasePrice(request.basePrice());

        return toView(showtimeRepository.save(showtime));
    }

    private AdminShowtimeView toView(Showtime showtime) {
        return new AdminShowtimeView(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getAuditorium().getId(),
                showtime.getAuditorium().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getStatus(),
                showtime.getBasePrice()
        );
    }
}
