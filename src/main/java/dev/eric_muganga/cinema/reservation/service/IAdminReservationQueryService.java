package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.reservation.dto.AdminReservationView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminReservationQueryService {
    Page<AdminReservationView> getReservations(String status, Pageable pageable);
}
