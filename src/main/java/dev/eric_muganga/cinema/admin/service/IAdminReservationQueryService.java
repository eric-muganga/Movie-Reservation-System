package dev.eric_muganga.cinema.admin.service;

import dev.eric_muganga.cinema.admin.dto.AdminReservationDetailView;
import dev.eric_muganga.cinema.reservation.dto.AdminReservationView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAdminReservationQueryService {
    Page<AdminReservationView> getReservations(String status, Pageable pageable);
    AdminReservationDetailView getReservationDetail(Long reservationId);
}
