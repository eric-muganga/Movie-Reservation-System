package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.reservation.dto.ReservationResult;

import java.util.List;

public interface IReservationService {
    ReservationResult reserveNow(
            String auth0Sub,
            Long showtimeId,
            List<Long> seatIds
    );
}
