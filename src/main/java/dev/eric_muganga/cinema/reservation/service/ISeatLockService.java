package dev.eric_muganga.cinema.reservation.service;

import java.util.List;

public interface ISeatLockService {
    void lockSeats(String auth0Sub, Long showtimeId, List<Long> seatIds);
}
