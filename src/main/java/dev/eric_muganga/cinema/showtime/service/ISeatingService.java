package dev.eric_muganga.cinema.showtime.service;


import dev.eric_muganga.cinema.showtime.dto.ShowtimeSeatingResponse;

public interface ISeatingService {
    ShowtimeSeatingResponse getSeatingForShowtime(Long showtimeId, String auth0Sub);
}
