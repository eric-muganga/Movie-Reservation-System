package dev.eric_muganga.cinema.reservation.controller;

import dev.eric_muganga.cinema.reservation.dto.SeatLockRequest;
import dev.eric_muganga.cinema.reservation.service.ISeatLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class SeatLockController {

    private final ISeatLockService seatLockService;

    @PostMapping("/locks")
    public ResponseEntity<?> lockSeats(@RequestHeader("X-Debug-User") String auth0Sub,
                                       @RequestBody SeatLockRequest request) {
        seatLockService.lockSeats(auth0Sub, request.showtimeId(), request.seatIds());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}