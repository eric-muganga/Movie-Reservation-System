package dev.eric_muganga.cinema.admin;

import dev.eric_muganga.cinema.CinemaApplication;
import dev.eric_muganga.cinema.movie.entity.Movie;
import dev.eric_muganga.cinema.movie.repository.MovieRepository;
import dev.eric_muganga.cinema.reservation.entity.Reservation;
import dev.eric_muganga.cinema.reservation.entity.ReservationSeat;
import dev.eric_muganga.cinema.reservation.entity.ReservationStatus;
import dev.eric_muganga.cinema.reservation.repository.ReservationRepository;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.showtime.entity.ShowtimeStatus;
import dev.eric_muganga.cinema.showtime.repository.ShowtimeRepository;
import dev.eric_muganga.cinema.user.entity.User;
import dev.eric_muganga.cinema.user.repository.UserRepository;
import dev.eric_muganga.cinema.venue.entity.Auditorium;
import dev.eric_muganga.cinema.venue.entity.Seat;
import dev.eric_muganga.cinema.venue.repository.AuditoriumRepository;
import dev.eric_muganga.cinema.venue.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@SpringBootTest(classes = CinemaApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional
class AdminReservationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("cinema_admin_test")
                    .withUsername("cinema")
                    .withPassword("cinema");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AuditoriumRepository auditoriumRepository;

    @Autowired
    private SeatRepository seatRepository;

    private Reservation confirmedReservation;
    private Reservation cancelledReservation;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        seatRepository.deleteAll();
        auditoriumRepository.deleteAll();
        userRepository.deleteAll();

        OffsetDateTime now = OffsetDateTime.now();

        User customer = new User();
        customer.setAuth0Sub("auth0|admin-test-user");
        customer.setEmail("customer@example.com");
        customer.setFullName("Customer Example");
        customer.setCreatedAt(now.minusDays(2));
        customer.setUpdatedAt(now.minusDays(2));
        customer = userRepository.save(customer);

        Auditorium auditorium = Auditorium.builder()
                .name("Screen 1 (test)")
                .totalRows(5)
                .totalCols(5)
                .build();
        auditorium = auditoriumRepository.save(auditorium);

        Seat seatA1 = new Seat();
        seatA1.setAuditorium(auditorium);
        seatA1.setRowLabel("A");
        seatA1.setSeatNumber(1);
        seatA1.setSeatType("STANDARD");
        seatA1 = seatRepository.save(seatA1);

        Seat seatA2 = new Seat();
        seatA2.setAuditorium(auditorium);
        seatA2.setRowLabel("A");
        seatA2.setSeatNumber(2);
        seatA2.setSeatType("STANDARD");
        seatA2 = seatRepository.save(seatA2);

        Seat seatA3 = new Seat();
        seatA3.setAuditorium(auditorium);
        seatA3.setRowLabel("A");
        seatA3.setSeatNumber(3);
        seatA3.setSeatType("STANDARD");
        seatA3 = seatRepository.save(seatA3);

        Movie movie = Movie.builder()
                .title("Inception")
                .description("A mind-bending sci-fi thriller.")
                .posterUrl("https://example.com/poster.jpg")
                .durationMinutes(148)
                .createdAt(now.minusDays(10))
                .updatedAt(now.minusDays(1))
                .build();
        movie = movieRepository.save(movie);

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .auditorium(auditorium)
                .startTime(OffsetDateTime.of(2026, 7, 27, 18, 30, 0, 0, ZoneOffset.UTC))
                .endTime(OffsetDateTime.of(2026, 7, 27, 21, 0, 0, 0, ZoneOffset.UTC))
                .status(ShowtimeStatus.SCHEDULED)
                .basePrice(new BigDecimal("12.50"))
                .build();
        showtime = showtimeRepository.save(showtime);

        confirmedReservation = new Reservation();
        confirmedReservation.setUser(customer);
        confirmedReservation.setShowtime(showtime);
        confirmedReservation.setStatus(ReservationStatus.CONFIRMED);
        confirmedReservation.setTotalAmount(new BigDecimal("37.50"));
        confirmedReservation.setCreatedAt(now.minusDays(1));
        confirmedReservation.setUpdatedAt(now.minusDays(1));

        ReservationSeat rs1 = new ReservationSeat();
        rs1.setReservation(confirmedReservation);
        rs1.setShowtime(showtime);
        rs1.setSeat(seatA1);
        rs1.setPrice(new BigDecimal("12.50"));

        ReservationSeat rs2 = new ReservationSeat();
        rs2.setReservation(confirmedReservation);
        rs2.setShowtime(showtime);
        rs2.setSeat(seatA2);
        rs2.setPrice(new BigDecimal("12.50"));

        ReservationSeat rs3 = new ReservationSeat();
        rs3.setReservation(confirmedReservation);
        rs3.setShowtime(showtime);
        rs3.setSeat(seatA3);
        rs3.setPrice(new BigDecimal("12.50"));

        confirmedReservation.setReservationSeats(List.of(rs1, rs2, rs3));
        confirmedReservation = reservationRepository.save(confirmedReservation);

        cancelledReservation = new Reservation();
        cancelledReservation.setUser(customer);
        cancelledReservation.setShowtime(showtime);
        cancelledReservation.setStatus(ReservationStatus.CANCELLED);
        cancelledReservation.setTotalAmount(new BigDecimal("12.50"));
        cancelledReservation.setCreatedAt(now.minusHours(12));
        cancelledReservation.setUpdatedAt(now.minusHours(12));
        cancelledReservation = reservationRepository.save(cancelledReservation);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reservationsList_rendersAdminReservationsPage() throws Exception {
        mockMvc.perform(get("/admin/reservations"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservations"))
                .andExpect(model().attributeExists("reservations"))
                .andExpect(model().attribute("title", "Reservations"))
                .andExpect(content().string(containsString("customer@example.com")))
                .andExpect(content().string(containsString("Inception")))
                .andExpect(content().string(containsString("CONFIRMED")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reservationDetail_rendersSeatLabelsAndReservationData() throws Exception {
        mockMvc.perform(get("/admin/reservations/{reservationId}", confirmedReservation.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation-detail"))
                .andExpect(model().attributeExists("reservation"))
                .andExpect(model().attribute("title", "Reservation Detail"))
                .andExpect(content().string(containsString("customer@example.com")))
                .andExpect(content().string(containsString("Inception")))
                .andExpect(content().string(containsString("A1")))
                .andExpect(content().string(containsString("A2")))
                .andExpect(content().string(containsString("A3")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelReservation_changesStatusAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/admin/reservations/{reservationId}/cancel", confirmedReservation.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reservations/" + confirmedReservation.getId()))
                .andExpect(flash().attribute("successMessage", "Reservation cancelled successfully."));

        Reservation updated = reservationRepository.findById(confirmedReservation.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancelReservation_whenAlreadyCancelled_keepsStatusAndReturnsMessage() throws Exception {
        mockMvc.perform(post("/admin/reservations/{reservationId}/cancel", cancelledReservation.getId())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/reservations/" + cancelledReservation.getId()))
                .andExpect(flash().attribute("successMessage",
                        "Reservation cannot be cancelled from status: CANCELLED"));

        Reservation unchanged = reservationRepository.findById(cancelledReservation.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}