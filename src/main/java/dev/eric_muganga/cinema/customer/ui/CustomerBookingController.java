package dev.eric_muganga.cinema.customer.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/showtimes")
public class CustomerBookingController {

    @GetMapping("/{showtimeId}/seats")
    public String seatSelection(
            @PathVariable Long showtimeId,
            Model model
    ) {
        model.addAttribute("showtimeId", showtimeId);
        return "customer/seat-selection";
    }


//    @GetMapping("/reservations/{reservationId}/confirmation")
//    public String reservationConfirmation(
//            @PathVariable Long reservationId,
//            Model model
//    ) {
//        model.addAttribute("reservationId", reservationId);
//        return "customer/reservation-confirmation";
//    }
}
