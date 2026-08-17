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
    void startCheckout_availableSeats_createsLocksAndPendingReservation() {
        String auth0Sub = "auth0|user-1";
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(1L, 2L);

        User user = user(10L, auth0Sub);
        Auditorium auditorium = auditorium(100L, "Screen 1");
        Showtime showtime = showtime(showtimeId, auditorium, new BigDecimal("12.50"));

        Seat seat1 = seat(1L, auditorium, "A", 1);
        Seat seat2 = seat(2L, auditorium, "A", 2);

        when(userRepository.findByAuth0Sub(auth0Sub)).thenReturn(Optional.of(user));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(seatRepository.findAllById(seatIds)).thenReturn(List.of(seat1, seat2));
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds))
                .thenReturn(List.of());
        when(seatLockRepository.findActiveLocksForShowtime(eq(showtimeId), any()))
                .thenReturn(List.of());

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(999L);
            return reservation;
        });

        when(seatLockRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResult result =
                reservationService.startCheckout(auth0Sub, showtimeId, seatIds);

        ArgumentCaptor<Reservation> reservationCaptor =
                ArgumentCaptor.forClass(Reservation.class);

        verify(reservationRepository).save(reservationCaptor.capture());

        Reservation savedReservation = reservationCaptor.getValue();

        assertThat(savedReservation.getStatus())
                .isEqualTo(ReservationStatus.PENDING);

        assertThat(savedReservation.getPaymentStatus())
                .isEqualTo(PaymentStatus.PENDING);

        assertThat(savedReservation.getPaidAt()).isNull();

        assertThat(result.reservationId()).isEqualTo(999L);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.showtimeId()).isEqualTo(showtimeId);
        assertThat(result.seats()).hasSize(2);
        assertThat(result.totalAmount()).isEqualByComparingTo("25.00");

        verify(seatLockRepository).expireLocks(any());
        verify(reservationSeatRepository)
                .findReservedSeatIdsForShowtime(showtimeId, seatIds);
        verify(seatLockRepository)
                .findActiveLocksForShowtime(eq(showtimeId), any());

        // Locks are persisted once and remain ACTIVE while payment is pending.
        verify(seatLockRepository, times(1)).saveAll(anyList());

        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void startCheckout_bookedSeats_throwsSeatConflictException() {
        String auth0Sub = "auth0|user-1";
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(1L, 2L);

        User user = user(10L, auth0Sub);
        Auditorium auditorium = auditorium(100L, "Screen 1");
        Showtime showtime = showtime(showtimeId, auditorium, new BigDecimal("12.50"));

        when(userRepository.findByAuth0Sub(auth0Sub)).thenReturn(Optional.of(user));
        when(showtimeRepository.findById(showtimeId)).thenReturn(Optional.of(showtime));
        when(seatRepository.findAllById(seatIds)).thenReturn(List.of(
                seat(1L, auditorium, "A", 1),
                seat(2L, auditorium, "A", 2)
        ));

        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds))
                .thenReturn(List.of(1L));

        assertThatThrownBy(() ->
                reservationService.startCheckout(auth0Sub, showtimeId, seatIds)
        )
                .isInstanceOf(SeatConflictException.class)
                .hasMessageContaining("already reserved");

        verify(seatLockRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void startCheckout_seatsLockedByOtherUser_throwsSeatConflictException() {
        String auth0Sub = "auth0|user-1";
        Long showtimeId = 1L;
        List<Long> seatIds = List.of(1L);

        User currentUser = user(10L, auth0Sub);
        User otherUser = user(20L, "auth0|other");

        Auditorium auditorium = auditorium(100L, "Screen 1");
        Showtime showtime = showtime(showtimeId, auditorium, new BigDecimal("12.50"));
        Seat seat = seat(1L, auditorium, "A", 1);

        SeatLock otherUserLock = SeatLock.builder()
                .id(1L)
                .user(otherUser)
                .showtime(showtime)
                .seat(seat)
                .lockedAt(OffsetDateTime.now())
                .lockExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .status(SeatLockStatus.ACTIVE)
                .build();

        when(userRepository.findByAuth0Sub(auth0Sub))
                .thenReturn(Optional.of(currentUser));
        when(showtimeRepository.findById(showtimeId))
                .thenReturn(Optional.of(showtime));
        when(seatRepository.findAllById(seatIds))
                .thenReturn(List.of(seat));
        when(reservationSeatRepository.findReservedSeatIdsForShowtime(showtimeId, seatIds))
                .thenReturn(List.of());
        when(seatLockRepository.findActiveLocksForShowtime(eq(showtimeId), any()))
                .thenReturn(List.of(otherUserLock));

        assertThatThrownBy(() ->
                reservationService.startCheckout(auth0Sub, showtimeId, seatIds)
        )
                .isInstanceOf(SeatConflictException.class)
                .hasMessageContaining("locked");

        verify(seatLockRepository, never()).saveAll(anyList());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void startCheckout_missingUser_throwsResourceNotFound() {
        when(userRepository.findByAuth0Sub("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                reservationService.startCheckout("missing", 1L, List.of(1L))
        )
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(
                showtimeRepository,
                seatRepository,
                reservationSeatRepository,
                seatLockRepository,
                reservationRepository
        );
    }

    @Test
    void confirmPayment_pendingReservation_marksPaidConfirmedAndConsumesLocks() {
        User user = user(10L, "auth0|user-1");
        Auditorium auditorium = auditorium(100L, "Screen 1");
        Showtime showtime = showtime(1L, auditorium, new BigDecimal("12.50"));
        Seat seat = seat(1L, auditorium, "A", 1);

        Reservation reservation = Reservation.builder()
                .id(50L)
                .user(user)
                .showtime(showtime)
                .status(ReservationStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(new BigDecimal("12.50"))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ReservationSeat reservationSeat = ReservationSeat.builder()
                .reservation(reservation)
                .showtime(showtime)
                .seat(seat)
                .price(new BigDecimal("12.50"))
                .build();

        reservation.setReservationSeats(List.of(reservationSeat));

        SeatLock activeLock = SeatLock.builder()
                .id(70L)
                .user(user)
                .showtime(showtime)
                .seat(seat)
                .lockedAt(OffsetDateTime.now())
                .lockExpiresAt(OffsetDateTime.now().plusMinutes(5))
                .status(SeatLockStatus.ACTIVE)
                .build();

        when(reservationRepository.findById(50L))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(seatLockRepository.findByShowtimeIdAndUserIdAndStatus(
                showtime.getId(),
                user.getId(),
                SeatLockStatus.ACTIVE
        )).thenReturn(List.of(activeLock));

        ReservationResult result =
                reservationService.confirmPayment(50L, "payment-ref-001");

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CONFIRMED);

        assertThat(reservation.getPaymentStatus())
                .isEqualTo(PaymentStatus.PAID);

        assertThat(reservation.getPaymentReference())
                .isEqualTo("payment-ref-001");

        assertThat(reservation.getPaidAt()).isNotNull();

        assertThat(activeLock.getStatus())
                .isEqualTo(SeatLockStatus.CONSUMED);

        assertThat(result.status()).isEqualTo("CONFIRMED");

        verify(reservationSeatRepository).saveAll(anyList());

        verify(seatLockRepository).saveAll(List.of(activeLock));
    }

    @Test
    void failPayment_pendingReservation_cancelsReservationAndReleasesLocks() {
        User user = user(10L, "auth0|user-1");
        Auditorium auditorium = auditorium(100L, "Screen 1");
        Showtime showtime = showtime(1L, auditorium, new BigDecimal("12.50"));
        Seat seat = seat(1L, auditorium, "A", 1);

        Reservation reservation = Reservation.builder()
                .id(50L)
                .user(user)
                .showtime(showtime)
                .status(ReservationStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ReservationSeat reservationSeat = ReservationSeat.builder()
                .reservation(reservation)
                .showtime(showtime)
                .seat(seat)
                .price(new BigDecimal("12.50"))
                .build();

        reservation.setReservationSeats(List.of(reservationSeat));

        when(reservationRepository.findById(50L))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.failPayment(50L, "payment-failed-001");

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);

        assertThat(reservation.getPaymentStatus())
                .isEqualTo(PaymentStatus.FAILED);

        assertThat(reservation.getPaymentReference())
                .isEqualTo("payment-failed-001");

        verify(seatLockRepository).releaseActiveLocksForUserAndShowtime(
                eq(user.getId()),
                eq(showtime.getId()),
                any(OffsetDateTime.class)
        );
    }

    private User user(Long id, String auth0Sub) {
        User user = new User();
        user.setId(id);
        user.setAuth0Sub(auth0Sub);
        return user;
    }

    private Auditorium auditorium(Long id, String name) {
        return Auditorium.builder()
                .id(id)
                .name(name)
                .totalRows(5)
                .totalCols(10)
                .build();
    }

    private Showtime showtime(
            Long id,
            Auditorium auditorium,
            BigDecimal basePrice
    ) {
        return Showtime.builder()
                .id(id)
                .auditorium(auditorium)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .basePrice(basePrice)
                .build();
    }

    private Seat seat(
            Long id,
            Auditorium auditorium,
            String rowLabel,
            int seatNumber
    ) {
        return Seat.builder()
                .id(id)
                .auditorium(auditorium)
                .rowLabel(rowLabel)
                .seatNumber(seatNumber)
                .seatType("STANDARD")
                .build();
    }
}
