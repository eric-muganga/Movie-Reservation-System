package dev.eric_muganga.cinema.admin.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
@RequestMapping("/movies")
public class CustomerMovieController {

    @GetMapping
    public String movies(
            @RequestParam(
                    value = "date",
                    required = false
            )
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Model model
    ) {
        model.addAttribute(
                "selectedDate",
                date != null ? date : LocalDate.now()
        );

        return "customer/movies";
    }
}
