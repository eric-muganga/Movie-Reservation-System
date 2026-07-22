package dev.eric_muganga.cinema.admin.service;

import dev.eric_muganga.cinema.admin.dto.AdminReservationActionResult;

public interface IAdminReservationCommandService {
    AdminReservationActionResult cancelReservation(Long reservationId);
}
