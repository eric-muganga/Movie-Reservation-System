package dev.eric_muganga.cinema.customer.ui;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/reservations")
public class CustomerReservationController {

    @GetMapping("/{reservationId}/confirmation")
    public String confirmation(
            @PathVariable Long reservationId,
            Model model
    ) {
        model.addAttribute("reservationId", reservationId);
        return "customer/reservation-confirmation";
    }
}