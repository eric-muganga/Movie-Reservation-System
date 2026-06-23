package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.SeatConflictException;
import dev.eric_muganga.cinema.reservation.dto.ReservationResult;
import dev.eric_muganga.cinema.reservation.dto.ReservedSeatDto;
import dev.eric_muganga.cinema.reservation.entity.*;
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
import java.util.*;
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

    // For now, hardcode lock duration; later make it configurable
    private static final int LOCK_MINUTES = 5;

    @Override
    @Transactional
    public ReservationResult reserveNow(String auth0Sub, Long showtimeId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds must not be empty");
        }

        // 1) Load user by auth0Sub
        User user = userRepository.findByAuth0Sub(auth0Sub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for auth0Sub: " + auth0Sub));

        // 2) Load showtime and validate it is not in the past
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Showtime not found: " + showtimeId));

        OffsetDateTime now = OffsetDateTime.now();
        if (showtime.getStartTime().isBefore(now)) {
            throw new IllegalStateException("Cannot reserve seats for a showtime in the past");
        }

        // 3) Load seats and ensure they exist and belong to the showtime's auditorium
        List<Seat> seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("Some seatIds do not exist");
        }

        Long auditoriumId = showtime.getAuditorium().getId();
        boolean allInSameAuditorium = seats.stream()
                .allMatch(seat -> seat.getAuditorium().getId().equals(auditoriumId));

        if (!allInSameAuditorium) {
            throw new IllegalArgumentException(
                    "All seats must belong to the same auditorium as the showtime");
        }

        // 4) Prevent double booking: check for already reserved seats
        List<Long> alreadyReservedSeatIds =
                reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds);

        if (!alreadyReservedSeatIds.isEmpty()) {
            throw new SeatConflictException(
                    "Some seats are already reserved for this showtime: " + alreadyReservedSeatIds);
        }

        // 5) Check for active seat locks held by someone else
        List<SeatLock> activeLocks = seatLockRepository.findActiveLocksForShowtime(showtimeId, now);

        Set<Long> lockedSeatIdsByOthers = activeLocks.stream()
                .filter(lock -> !lock.getUser().getId().equals(user.getId()))
                .map(lock -> lock.getSeat().getId())
                .collect(Collectors.toSet());

        if (!Collections.disjoint(lockedSeatIdsByOthers, new HashSet<>(seatIds))) {
            throw new SeatConflictException(
                    "Some seats are currently locked by another user");
        }

        // 6) Create/refresh locks for this user and these seats
        OffsetDateTime expiresAt = now.plusMinutes(LOCK_MINUTES);
        List<SeatLock> newLocks = new ArrayList<>();

        for (Seat seat : seats) {
            SeatLock lock = SeatLock.builder()
                    .user(user)
                    .showtime(showtime)
                    .seat(seat)
                    .lockedAt(now)
                    .lockExpiresAt(expiresAt)
                    .status(SeatLockStatus.ACTIVE)
                    .build();
            newLocks.add(lock);
        }

        seatLockRepository.saveAll(newLocks);

        // 7) Create reservation
        BigDecimal basePrice = showtime.getBasePrice();
        BigDecimal totalAmount = basePrice.multiply(BigDecimal.valueOf(seats.size()));

        Reservation reservation = Reservation.builder()
                .user(user)
                .showtime(showtime)
                .status(ReservationStatus.CONFIRMED)
                .totalAmount(totalAmount)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        // 8) Create reservation seats
        List<ReservationSeat> reservationSeats = seats.stream()
                .map(seat -> ReservationSeat.builder()
                        .reservation(savedReservation)
                        .seat(seat)
                        .showtime(showtime)
                        .price(basePrice)
                        .build())
                .toList();

        reservationSeatRepository.saveAll(reservationSeats);

        // Optionally: update locks to CONSUMED (if your SeatLockStatus supports it)
        newLocks.forEach(lock -> lock.setStatus(SeatLockStatus.CONSUMED));
        seatLockRepository.saveAll(newLocks);

        // 9) Map to ReservationResult
        List<ReservedSeatDto> seatDtos = reservationSeats.stream()
                .map(rs -> new ReservedSeatDto(
                        rs.getSeat().getId(),
                        rs.getSeat().getRowLabel(),
                        rs.getSeat().getSeatNumber(),
                        rs.getPrice()
                ))
                .toList();

        return new ReservationResult(
                savedReservation.getId(),
                savedReservation.getStatus().name(),
                showtime.getId(),
                savedReservation.getCreatedAt(),
                savedReservation.getTotalAmount(),
                seatDtos
        );
    }

    @Override
    @Transactional
    public void cancelReservation(String auth0Sub, Long reservationId) {
        // 1) Load user
        User user = userRepository.findByAuth0Sub(auth0Sub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for auth0Sub: " + auth0Sub));

        // 2) Load reservation
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found: " + reservationId));

        // 3) Ensure reservation belongs to user
        if (!reservation.getUser().getId().equals(user.getId())) {
            // Use 404 to avoid leaking existence of others' reservations
            throw new ResourceNotFoundException("Reservation not found: " + reservationId);
        }

        // 4) If already cancelled, no-op (idempotent)
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }

        // 5) Mark as cancelled
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(OffsetDateTime.now());
        reservationRepository.save(reservation);

        // 6) Optionally: free locks or mark reservation seats as cancelled.
        // Seating logic should only treat CONFIRMED reservations as reserved.
    }
}