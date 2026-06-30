package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.SeatConflictException;
import dev.eric_muganga.cinema.reservation.dto.ReservationResult;
import dev.eric_muganga.cinema.reservation.entity.Reservation;
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
import dev.eric_muganga.cinema.venue.entity.Auditorium;
import dev.eric_muganga.cinema.venue.entity.Seat;
import dev.eric_muganga.cinema.venue.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceImplTest {
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private SeatLockRepository seatLockRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationSeatRepository reservationSeatRepository;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void reserveNow_availableSeats_createLocksAndConfirmedReservation() {
        // Arrange
        String auth0Sub = "auth0|user-1";
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(1L, 2L);

        OffsetDateTime now = OffsetDateTime.now().plusMinutes(1); // showtime in future

        User user = new User();
        user.setId(10L);
        user.setAuth0Sub(auth0Sub);

        Auditorium auditorium = Auditorium.builder()
                .id(100L)
                .name("Screen 1")
                .totalRows(5)
                .totalCols(10)
                .build();

        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .auditorium(auditorium)
                .startTime(now.plusHours(1))   // definitely in future
                .endTime(now.plusHours(3))
                .basePrice(BigDecimal.valueOf(12.50))
                .build();

        Seat seat1 = Seat.builder()
                .id(1L)
                .auditorium(auditorium)
                .rowLabel("A")
                .seatNumber(1)
                .seatType("STANDARD")
                .build();

        Seat seat2 = Seat.builder()
                .id(2L)
                .auditorium(auditorium)
                .rowLabel("A")
                .seatNumber(2)
                .seatType("STANDARD")
                .build();

        when(userRepository.findByAuth0Sub(auth0Sub)).thenReturn(Optional.of(user));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(seatRepository.findAllById(seatIds)).thenReturn(List.of(seat1, seat2));

        // AVAILABLE: no reserved seats, no active locks
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds))
                .thenReturn(List.of());
        when(seatLockRepository.findActiveLocksForShowtime(eq(showtimeId), any()))
                .thenReturn(List.of());

        // Reservation persistence: echo back with id
        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation r = invocation.getArgument(0);
                    r.setId(999L);
                    return r;
                });

        // Act
        ReservationResult result = reservationService.reserveNow(auth0Sub, showtimeId, seatIds);

        // Assert: result
        assertThat(result.reservationId()).isEqualTo(999L);
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.showtimeId()).isEqualTo(showtimeId);
        assertThat(result.seats()).hasSize(2);
        assertThat(result.totalAmount())
                .isEqualTo(BigDecimal.valueOf(12.50).multiply(BigDecimal.valueOf(2)));

        // Assert: interactions reflect AVAILABLE → LOCKED → BOOKED
        // expireLocks called
        verify(seatLockRepository).expireLocks(any());

        // reserved-seat check called
        verify(reservationSeatRepository)
                .findReservedSeatIdsForShowtime(showtimeId, seatIds);

        // active lock check called
        verify(seatLockRepository)
                .findActiveLocksForShowtime(eq(showtimeId), any());

        // locks saved then marked CONSUMED
        ArgumentCaptor<List<SeatLock>> lockCaptor = ArgumentCaptor.forClass(List.class);
        verify(seatLockRepository, times(2)).saveAll(lockCaptor.capture());

        // reservation seats persisted
        verify(reservationSeatRepository).saveAll(anyList());
        verify(reservationRepository).save(reservationCaptor.capture());

        Reservation persisted = reservationCaptor.getValue();
        assertThat(persisted.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void reserveNow_bookedSeats_throwSeatConflictException() {
        String auth0Sub = "auth0|user-1";
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(1L, 2L);

        User user = new User();
        user.setId(10L);
        user.setAuth0Sub(auth0Sub);

        Auditorium auditorium = Auditorium.builder()
                .id(100L)
                .name("Screen 1")
                .totalRows(5)
                .totalCols(10)
                .build();

        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .auditorium(auditorium)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .basePrice(BigDecimal.valueOf(12.50))
                .build();

        when(userRepository.findByAuth0Sub(auth0Sub)).thenReturn(Optional.of(user));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(seatRepository.findAllById(seatIds)).thenReturn(List.of(
                Seat.builder().id(1L).auditorium(auditorium).rowLabel("A").seatNumber(1).seatType("STANDARD").build(),
                Seat.builder().id(2L).auditorium(auditorium).rowLabel("A").seatNumber(2).seatType("STANDARD").build()
        ));

        // BOOKED: reserved-seat repository reports seats as already reserved
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds))
                .thenReturn(List.of(1L));

        assertThatThrownBy(() -> reservationService.reserveNow(auth0Sub, showtimeId, seatIds))
                .isInstanceOf(SeatConflictException.class)
                .hasMessageContaining("already reserved");

        // confirm we never go on to create locks or reservations
        verify(seatLockRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveNow_seatsLockedByOtherUser_throwSeatConflictException() {
        String auth0Sub = "auth0|user-1";
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(1L);

        User currentUser = new User();
        currentUser.setId(10L);
        currentUser.setAuth0Sub(auth0Sub);

        User otherUser = new User();
        otherUser.setId(20L);
        otherUser.setAuth0Sub("auth0|other");

        Auditorium auditorium = Auditorium.builder()
                .id(100L)
                .name("Screen 1")
                .totalRows(5)
                .totalCols(10)
                .build();

        Showtime showtime = Showtime.builder()
                .id(showtimeId)
                .auditorium(auditorium)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .basePrice(BigDecimal.valueOf(12.50))
                .build();

        Seat seat = Seat.builder()
                .id(1L)
                .auditorium(auditorium)
                .rowLabel("A")
                .seatNumber(1)
                .seatType("STANDARD")
                .build();

        when(userRepository.findByAuth0Sub(auth0Sub)).thenReturn(Optional.of(currentUser));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(seatRepository.findAllById(seatIds)).thenReturn(List.of(seat));

        // AVAILABLE in terms of booked seats
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds))
                .thenReturn(List.of());

        // LOCKED by other user
        SeatLock otherLock = SeatLock.builder()
                .id(1L)
                .user(otherUser)
                .showtime(showtime)
                .seat(seat)
                .lockedAt(OffsetDateTime.now())
                .lockExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .status(SeatLockStatus.ACTIVE)
                .build();

        when(seatLockRepository.findActiveLocksForShowtime(eq(showtimeId), any()))
                .thenReturn(List.of(otherLock));

        assertThatThrownBy(() -> reservationService.reserveNow(auth0Sub, showtimeId, seatIds))
                .isInstanceOf(SeatConflictException.class)
                .hasMessageContaining("locked");

        verify(seatLockRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserveNow_missingUser_throwsResourceNotFound() {
        when(userRepository.findByAuth0Sub("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserveNow("missing", 1L, List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(showtimeRepository, seatRepository, reservationSeatRepository,
                seatLockRepository, reservationRepository);
    }
}
