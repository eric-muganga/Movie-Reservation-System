package dev.eric_muganga.cinema.showtime.controller;

import dev.eric_muganga.cinema.showtime.dto.ShowtimeSeatingResponse;
import dev.eric_muganga.cinema.showtime.service.ISeatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class SeatingController {
    private final ISeatingService seatingService;

    @GetMapping("/{showtimeId}/seating")
    public ShowtimeSeatingResponse getSeating(
            @PathVariable Long showtimeId,
            @RequestHeader(value = "X-Debug-User", required = false) String auth0Sub
    ){
        return seatingService.getSeatingForShowtime(showtimeId, auth0Sub);
    }
}
