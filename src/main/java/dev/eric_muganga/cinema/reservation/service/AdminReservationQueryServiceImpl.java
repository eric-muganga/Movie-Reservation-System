package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.reservation.dto.AdminReservationView;
import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import dev.eric_muganga.cinema.reservation.repository.ReservationRepository;
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
}
