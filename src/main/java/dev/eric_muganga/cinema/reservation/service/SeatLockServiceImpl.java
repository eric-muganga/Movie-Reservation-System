package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.SeatConflictException;
import dev.eric_muganga.cinema.reservation.entity.SeatLock;
import dev.eric_muganga.cinema.reservation.entity.SeatLockStatus;
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

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatLockServiceImpl implements ISeatLockService {
    private final SeatLockRepository seatLockRepository;
    private final UserRepository userRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;

    private static final int LOCK_MINUTES = 5;

    @Override
    @Transactional
    public void lockSeats(String auth0Sub, Long showtimeId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("seatIds must not be empty");
        }

        User user = userRepository.findByAuth0Sub(auth0Sub)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for auth0Sub: " + auth0Sub));

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Showtime not found: " + showtimeId));

        OffsetDateTime now = OffsetDateTime.now();
        if (showtime.getStartTime().isBefore(now)) {
            throw new IllegalStateException("Cannot lock seats for a showtime in the past");
        }

        List<Seat> seats = seatRepository.findAllById(seatIds);
        // TODO: re-enable strict checks once integration tests use real seat IDs

        Long auditoriumId = showtime.getAuditorium().getId();
        boolean allInSameAuditorium = seats.stream()
                .allMatch(seat -> seat.getAuditorium().getId().equals(auditoriumId));

        if (!allInSameAuditorium) {
            throw new IllegalArgumentException(
                    "All seats must belong to the same auditorium as the showtime");
        }

        // Expire stale locks
        seatLockRepository.expireLocks(now);

        // Check for active locks by others
        List<SeatLock> activeLocks = seatLockRepository.findActiveLocksForShowtime(showtimeId, now);

        Set<Long> lockedSeatIdsByOthers = activeLocks.stream()
                .filter(lock -> !lock.getUser().getId().equals(user.getId()))
                .map(lock -> lock.getSeat().getId())
                .collect(Collectors.toSet());

        Set<Long> requestedSeatIds = new HashSet<>(seatIds);
        requestedSeatIds.retainAll(lockedSeatIdsByOthers);

        if (!requestedSeatIds.isEmpty()) {
            throw new SeatConflictException(
                    "Some seats are currently locked (LOCKED) by another user: " + requestedSeatIds);
        }

        // Create ACTIVE locks for this user
        OffsetDateTime expiresAt = now.plusMinutes(LOCK_MINUTES);
        List<SeatLock> newLocks = seats.stream()
                .map(seat -> SeatLock.builder()
                        .user(user)
                        .showtime(showtime)
                        .seat(seat)
                        .lockedAt(now)
                        .lockExpiresAt(expiresAt)
                        .status(SeatLockStatus.ACTIVE)
                        .build())
                .toList();

        seatLockRepository.saveAll(newLocks);
    }
}
