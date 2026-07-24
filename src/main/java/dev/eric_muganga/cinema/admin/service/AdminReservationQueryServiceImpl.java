package dev.eric_muganga.cinema.admin.service;

import dev.eric_muganga.cinema.admin.dto.AdminReservationDetailView;
import dev.eric_muganga.cinema.reservation.dto.AdminReservationView;
import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import dev.eric_muganga.cinema.reservation.repository.ReservationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminReservationQueryServiceImpl implements IAdminReservationQueryService {
    private final ReservationRepository reservationRepository;

    @Override
    public Page<AdminReservationView> getReservations(String status, Pageable pageable) {
        Page<Reservation> page =
                (status == null || status.isBlank())
                        ? reservationRepository.findAll(pageable)
                        : reservationRepository.findByStatus(ReservationStatus.valueOf(status), pageable);

        return page.map(reservation -> new AdminReservationView(
                reservation.getId(),
                reservation.getUser().getEmail(),
                reservation.getShowtime().getMovie().getTitle(),
                reservation.getShowtime().getStartTime(),
                reservation.getStatus().name(),
                reservation.getTotalAmount(),
                reservation.getCreatedAt()
        ));
    }

    @Override
    public AdminReservationDetailView getReservationDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found: " + reservationId));

        return new AdminReservationDetailView(
                reservation.getId(),
                reservation.getUser().getEmail(),
                reservation.getShowtime().getMovie().getTitle(),
                reservation.getShowtime().getStartTime(),
                reservation.getStatus().name(),
                reservation.getTotalAmount(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt(),
                reservation.getSeats().stream()
                        .map(reservationSeat -> reservationSeat.getSeat().getRowLabel()
                                + reservationSeat.getSeat().getSeatNumber())
                        .sorted()
                        .toList(),
                reservation.getStatus() == ReservationStatus.PENDING
                        || reservation.getStatus() == ReservationStatus.CONFIRMED
        );
    }
}
