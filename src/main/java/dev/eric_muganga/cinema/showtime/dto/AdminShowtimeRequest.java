package dev.eric_muganga.cinema.showtime.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminShowtimeRequest(

        @NotNull(message = "Movie is required")
        Long movieId,

        @NotNull(message = "Auditorium is required")
        Long auditoriumId,

        @NotNull(message = "Start time is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endTime,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
        BigDecimal basePrice
) {

    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }

        return endTime.isAfter(startTime);
    }
}