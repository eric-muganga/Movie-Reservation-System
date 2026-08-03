package dev.eric_muganga.cinema.reservation.service;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.SeatConflictException;
import dev.eric_muganga.cinema.reservation.dto.ReservationResult;
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
class ReservationServiceImplTest {

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
    void startCheckout_availableSeats_createLocksAndConfirmedReservation() {
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
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds)).thenReturn(List.of());
        when(seatLockRepository.findActiveLocksForShowtime(eq(showtimeId), any())).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(999L);
            return r;
        });
        when(reservationSeatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(seatLockRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResult result = reservationService.startCheckout(auth0Sub, showtimeId, seatIds);

        assertThat(result.reservationId()).isEqualTo(999L);
        assertThat(result.status()).isEqualTo("CONFIRMED");
        assertThat(result.showtimeId()).isEqualTo(showtimeId);
        assertThat(result.seats()).hasSize(2);
        assertThat(result.totalAmount()).isEqualTo(BigDecimal.valueOf(12.50).multiply(BigDecimal.valueOf(2)));

        verify(seatLockRepository).expireLocks(any());
        verify(reservationSeatRepository).findReservedSeatIdsForShowtime(showtimeId, seatIds);
        verify(seatLockRepository).findActiveLocksForShowtime(eq(showtimeId), any());
        verify(seatLockRepository, times(2)).saveAll(anyList());
        verify(reservationSeatRepository).saveAll(anyList());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void startCheckout_bookedSeats_throwSeatConflictException() {
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
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> reservationService.startCheckout(auth0Sub, showtimeId, seatIds))
                .isInstanceOf(SeatConflictException.class)
                .hasMessageContaining("already reserved");

        verify(seatLockRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void startCheckout_seatsLockedByOtherUser_throwSeatConflictException() {
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
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds)).thenReturn(List.of());

        SeatLock otherLock = SeatLock.builder()
                .id(1L)
                .user(otherUser)
                .showtime(showtime)
                .seat(seat)
                .lockedAt(OffsetDateTime.now())
                .lockExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .status(SeatLockStatus.ACTIVE)
                .build();

        when(seatLockRepository.findActiveLocksForShowtime(eq(showtimeId), any())).thenReturn(List.of(otherLock));

        assertThatThrownBy(() -> reservationService.startCheckout(auth0Sub, showtimeId, seatIds))
                .isInstanceOf(SeatConflictException.class)
                .hasMessageContaining("locked");

        verify(seatLockRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void startCheckout_missingUser_throwsResourceNotFound() {
        when(userRepository.findByAuth0Sub("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.startCheckout("missing", 1L, List.of(1L)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(showtimeRepository, seatRepository, reservationSeatRepository, seatLockRepository, reservationRepository);
    }
}
