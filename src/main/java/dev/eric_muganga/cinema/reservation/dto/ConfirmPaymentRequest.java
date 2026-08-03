package dev.eric_muganga.cinema.reservation.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank String paymentReference
) {}
