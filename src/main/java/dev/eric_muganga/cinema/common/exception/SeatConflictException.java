package dev.eric_muganga.cinema.common.exception;

public class SeatConflictException extends RuntimeException {
    public SeatConflictException(String message) {
        super(message);
    }
}
