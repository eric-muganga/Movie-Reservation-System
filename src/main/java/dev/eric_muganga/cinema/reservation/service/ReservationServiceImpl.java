package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.SeatConflictException;
import dev.eric_muganga.cinema.reservation.dto.ReservationResult;
import dev.eric_muganga.cinema.reservation.dto.ReservedSeatDto;
import dev.eric_muganga.cinema.reservation.entity.PaymentStatus;
import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationSeat;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import dev.eric_muganga.cinema.reservation.entity.SeatLock;
import dev.eric_muganga.cinema.reservation.entity.SeatLockStatus;
import dev.eric_muganga.cinema.reservation.repository.ReservationRepository;
import dev.eric_muganga.cinema.reservation.repository.ReservationSeatRepository;
import dev.eric_muganga.cinema.reservation.repository.SeatLockRepository;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.showtime.repository.ShowtimeRepository;
import dev.eric_muganga.cinema.user.entity.User;
import dev.eric_muganga.cinema.user.repository.UserRepository;
import dev.eric_muganga.cinema.venue.entity.Seat;
import dev.eric_muganga.cinema.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements IReservationService {

    private final ReservationRepository reservationRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatLockRepository seatLockRepository;
    private final UserRepository userRepository;
    private final ReservationSeatRepository reservationSeatRepository;

    @Override
    @Transactional
    public ReservationResult startCheckout(String auth0Sub, Long showtimeId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds must not be empty");
        }

        User user = userRepository.findByAuth0Sub(auth0Sub)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for auth0Sub: " + auth0Sub));

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + showtimeId));

        OffsetDateTime now = OffsetDateTime.now();
        if (showtime.getStartTime().isBefore(now)) {
            throw new IllegalStateException("Cannot reserve seats for a showtime in the past");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more seatIds are invalid");
        }

        Long auditoriumId = showtime.getAuditorium().getId();
        boolean allInSameAuditorium = seats.stream().allMatch(seat -> seat.getAuditorium().getId().equals(auditoriumId));
        if (!allInSameAuditorium) {
            throw new IllegalArgumentException("All seats must belong to the same auditorium as the showtime");
        }

        seatLockRepository.expireLocks(now);

        List<Long> alreadyReservedSeatIds = reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds);
        if (!alreadyReservedSeatIds.isEmpty()) {
            throw new SeatConflictException("Some seats are already reserved (BOOKED) for this showtime: " + alreadyReservedSeatIds);
        }

        List<SeatLock> activeLocks = seatLockRepository.findActiveLocksForShowtime(showtimeId, now);
        Set<Long> requestedSeatIds = new HashSet<>(seatIds);

        Set<Long> lockedSeatIdsByOthers = activeLocks.stream()
                .filter(lock -> !lock.getUser().getId().equals(user.getId()))
                .map(lock -> lock.getSeat().getId())
                .collect(Collectors.toSet());

        Set<Long> conflictingLockedSeats = new HashSet<>(requestedSeatIds);
        conflictingLockedSeats.retainAll(lockedSeatIdsByOthers);
        if (!conflictingLockedSeats.isEmpty()) {
            throw new SeatConflictException("Some seats are currently locked (LOCKED) by another user: " + conflictingLockedSeats);
        }

        BigDecimal basePrice = showtime.getBasePrice();
        BigDecimal totalAmount = basePrice.multiply(BigDecimal.valueOf(seats.size()));
        OffsetDateTime lockExpiresAt = now.plusMinutes(5);

        List<SeatLock> newLocks = seats.stream()
                .map(seat -> SeatLock.builder()
                        .user(user)
                        .showtime(showtime)
                        .seat(seat)
                        .lockedAt(now)
                        .lockExpiresAt(lockExpiresAt)
                        .status(SeatLockStatus.ACTIVE)
                        .build())
                .toList();
        seatLockRepository.saveAll(newLocks);

        Reservation reservation = Reservation.builder()
                .user(user)
                .showtime(showtime)
                .status(ReservationStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(totalAmount)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);


        return new ReservationResult(
                savedReservation.getId(),
                savedReservation.getStatus().name(),
                showtime.getId(),
                savedReservation.getCreatedAt(),
                savedReservation.getTotalAmount(),
                seats.stream()
                        .map(seat -> new ReservedSeatDto(
                                seat.getId(),
                                seat.getRowLabel(),
                                seat.getSeatNumber(),
                                basePrice
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional
    public ReservationResult confirmPayment(Long reservationId, String paymentReference) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Reservation not found: " + reservationId)
                );

        if (reservation.getPaymentStatus() == PaymentStatus.PAID) {
            return toResult(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot confirm payment for a cancelled reservation"
            );
        }

        OffsetDateTime now = OffsetDateTime.now();

        List<SeatLock> activeLocks = seatLockRepository
                .findByShowtimeIdAndUserIdAndStatus(
                        reservation.getShowtime().getId(),
                        reservation.getUser().getId(),
                        SeatLockStatus.ACTIVE
                );

        if (activeLocks.isEmpty()) {
            throw new SeatConflictException(
                    "The seat hold has expired. Please select seats again."
            );
        }

        List<ReservationSeat> reservationSeats = activeLocks.stream()
                .map(lock -> ReservationSeat.builder()
                        .reservation(reservation)
                        .showtime(reservation.getShowtime())
                        .seat(lock.getSeat())
                        .price(reservation.getShowtime().getBasePrice())
                        .build())
                .toList();

        reservationSeatRepository.saveAll(reservationSeats);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setPaymentReference(paymentReference);
        reservation.setPaidAt(now);
        reservation.setUpdatedAt(now);

        Reservation savedReservation = reservationRepository.save(reservation);

        activeLocks.forEach(lock -> lock.setStatus(SeatLockStatus.CONSUMED));
        seatLockRepository.saveAll(activeLocks);

        return toResult(savedReservation);
    }

    @Override
    @Transactional
    public void failPayment(Long reservationId, String paymentReference) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found: " + reservationId
                        )
                );

        if (reservation.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot fail payment for a paid reservation"
            );
        }

        reservation.setPaymentStatus(PaymentStatus.FAILED);
        reservation.setPaymentReference(paymentReference);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(OffsetDateTime.now());

        reservationRepository.save(reservation);

        seatLockRepository.releaseActiveLocksForUserAndShowtime(
                reservation.getUser().getId(),
                reservation.getShowtime().getId(),
                OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void cancelReservation(String auth0Sub, Long reservationId) {
        User user = userRepository.findByAuth0Sub(auth0Sub)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found for auth0Sub: " + auth0Sub
                        )
                );

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Reservation not found: " + reservationId
                        )
                );

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Reservation not found: " + reservationId);
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }

        if (reservation.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException(
                    "Paid reservations cannot be cancelled through this endpoint"
            );
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(OffsetDateTime.now());

        reservationRepository.save(reservation);

        seatLockRepository.releaseActiveLocksForUserAndShowtime(
                reservation.getUser().getId(),
                reservation.getShowtime().getId(),
                OffsetDateTime.now()
        );
    }

    private ReservationResult toResult(Reservation reservation) {
        List<ReservedSeatDto> seats = reservation.getReservationSeats() == null
                ? List.of()
                : reservation.getReservationSeats().stream()
                .map(rs -> new ReservedSeatDto(
                        rs.getSeat().getId(),
                        rs.getSeat().getRowLabel(),
                        rs.getSeat().getSeatNumber(),
                        rs.getPrice()
                ))
                .toList();

        return new ReservationResult(
                reservation.getId(),
                reservation.getStatus().name(),
                reservation.getShowtime().getId(),
                reservation.getCreatedAt(),
                reservation.getTotalAmount(),
                seats
        );
    }


    private void releaseReservationLocks(Reservation reservation) {
        List<SeatLock> activeLocks = seatLockRepository
                .findByShowtimeIdAndUserIdAndStatus(
                        reservation.getShowtime().getId(),
                        reservation.getUser().getId(),
                        SeatLockStatus.ACTIVE
                );

        if (activeLocks.isEmpty()) {
            return;
        }

        activeLocks.forEach(lock -> lock.setStatus(SeatLockStatus.EXPIRED));
        seatLockRepository.saveAll(activeLocks);
    }
}