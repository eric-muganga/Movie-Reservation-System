package dev.eric_muganga.cinema.common.error;

import dev.eric_muganga.cinema.common.exception.ResourceNotFoundException;
import dev.eric_muganga.cinema.common.exception.SeatConflictException;
import dev.eric_muganga.cinema.common.exception.ShowtimeConflictException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ApiErrorResponse buildError(HttpStatus status, String message, WebRequest request) {
        String path = request.getDescription(false).replace("uri=", "");
        return new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }

    // 404 for missing entities (custom or JPA)
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(buildError(status, ex.getMessage(), request), status);
    }

    // 400 for validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                             WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Validation failed");
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(buildError(status, message, request), status);
    }

    @ExceptionHandler({SeatConflictException.class, ShowtimeConflictException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(
            RuntimeException ex,
            WebRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        return new ResponseEntity<>(buildError(status, ex.getMessage(), request), status);
    }

    // Fallback 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(buildError(status, "Unexpected error", request), status);
    }
}