package dev.eric_muganga.cinema.showtime.service;

import dev.eric_muganga.cinema.movie.dto.AdminMovieView;
import dev.eric_muganga.cinema.showtime.dto.AdminShowtimeRequest;
import dev.eric_muganga.cinema.showtime.dto.AdminShowtimeView;

import java.util.List;

public interface IAdminShowtimeService {
    List<AdminShowtimeView> getAllShowtimes();

    AdminShowtimeView getShowtime(Long showtimeId);

    AdminShowtimeView createShowtime(AdminShowtimeRequest request);

    AdminShowtimeView updateShowtime(Long showtimeId, AdminShowtimeRequest request);
}
