package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.reservation.dto.ReservationResult;

import java.util.List;

public interface IReservationService {
    ReservationResult startCheckout(String auth0Sub, Long showtimeId, List<Long> seatIds);
    ReservationResult confirmPayment(Long reservationId, String paymentReference);
    void failPayment(Long reservationId, String paymentReference);
    void cancelReservation(String auth0Sub, Long reservationId);
}
