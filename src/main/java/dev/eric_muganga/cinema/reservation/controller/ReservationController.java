package dev.eric_muganga.cinema.reservation.controller;

import dev.eric_muganga.cinema.reservation.dto.CreateReservationRequest;
import dev.eric_muganga.cinema.reservation.dto.ReservationResult;
import dev.eric_muganga.cinema.reservation.service.IReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final IReservationService reservationService;

    @PostMapping
    public ReservationResult reserveNow(
            // Temporary: user id from header; later replace with JWT-based auth
            @RequestHeader("X-Debug-User") String auth0Sub,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return reservationService.reserveNow(
                auth0Sub,
                request.showtimeId(),
                request.seatIds()
        );
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Void> cancelReservation(
            @RequestHeader("X-Debug-User") String auth0Sub,
            @PathVariable Long reservationId
    ) {
        reservationService.cancelReservation(auth0Sub, reservationId);
        return ResponseEntity.noContent().build();
    }

}