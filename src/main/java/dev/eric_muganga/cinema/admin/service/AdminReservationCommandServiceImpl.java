package dev.eric_muganga.cinema.admin.service;

import dev.eric_muganga.cinema.admin.dto.AdminReservationActionResult;
import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import dev.eric_muganga.cinema.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminReservationCommandServiceImpl implements IAdminReservationCommandService {
    private final ReservationRepository reservationRepository;

    @Override
    @Transactional
    public AdminReservationActionResult cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            return new AdminReservationActionResult(
                    reservation.getId(),
                    reservation.getStatus().name(),
                    "Reservation cannot be cancelled from status: " + reservation.getStatus().name()
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(OffsetDateTime.now());

        return new AdminReservationActionResult(
                reservation.getId(),
                reservation.getStatus().name(),
                "Reservation cancelled successfully."
        );
    }
}
