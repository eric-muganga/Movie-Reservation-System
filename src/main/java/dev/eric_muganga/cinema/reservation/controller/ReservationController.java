package dev.eric_muganga.cinema.reservation.controller;

import dev.eric_muganga.cinema.reservation.dto.ConfirmPaymentRequest;
import dev.eric_muganga.cinema.reservation.dto.CreateReservationRequest;
import dev.eric_muganga.cinema.reservation.dto.ReservationResult;
import dev.eric_muganga.cinema.reservation.service.IReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final IReservationService reservationService;

    @PostMapping
    public ReservationResult startCheckout(
            @RequestHeader("X-Debug-User") String auth0Sub,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return reservationService.startCheckout(
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

    @PostMapping("/{reservationId}/confirm-payment")
    public ResponseEntity<ReservationResult> confirmPayment(
            @PathVariable Long reservationId,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        ReservationResult response =
                reservationService.confirmPayment(reservationId, request.paymentReference());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{reservationId}/fail-payment")
    public ResponseEntity<Void> failPayment(
            @PathVariable Long reservationId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String reason = body != null
                ? body.getOrDefault("reason", "PAYMENT_FAILED")
                : "PAYMENT_FAILED";

        reservationService.failPayment(reservationId, reason);
        return ResponseEntity.noContent().build();
    }

}