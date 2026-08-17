package dev.eric_muganga.cinema.reservation;

import dev.eric_muganga.cinema.CinemaApplication;
import dev.eric_muganga.cinema.reservation.entity.SeatLock;
import dev.eric_muganga.cinema.reservation.entity.SeatLockStatus;
import dev.eric_muganga.cinema.reservation.repository.SeatLockRepository;
import dev.eric_muganga.cinema.user.entity.User;
import dev.eric_muganga.cinema.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CinemaApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReservationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("cinema_db_test")
                    .withUsername("cinema")
                    .withPassword("cinema");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private static final String USER_1 = "auth0|payment-user-1";
    private static final String USER_2 = "auth0|payment-user-2";
    private static final long SHOWTIME_ID = 3L;

    @BeforeAll
    void seedUsers() {
        seedUser(USER_1);
        seedUser(USER_2);
    }

    @Test
    void startCheckout_createsPendingReservation() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", USER_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.showtimeId").value((int) SHOWTIME_ID))
                .andExpect(jsonPath("$.seats", hasSize(1)))
                .andExpect(jsonPath("$.totalAmount", greaterThan(0.0)));
    }

    @Test
    void confirmPayment_confirmsPendingReservation() throws Exception {
        long reservationId = createPendingReservation(USER_1, 8);

        mockMvc.perform(post("/api/reservations/{reservationId}/confirm-payment", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentReference": "test-payment-success-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.showtimeId").value((int) SHOWTIME_ID));
    }

    @Test
    void confirmedReservation_blocksAnotherReservationForSameSeat() throws Exception {
        long reservationId = createPendingReservation(USER_1, 9);

        mockMvc.perform(post("/api/reservations/{reservationId}/confirm-payment", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentReference": "test-payment-success-002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", USER_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 9)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
    }

    @Test
    void pendingReservation_blocksAnotherUserUntilPaymentCompletesOrFails() throws Exception {
        createPendingReservation(USER_1, 10);

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", USER_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 10)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("locked")));
    }

    @Test
    void failedPayment_cancelsReservationAndReleasesSeats() throws Exception {
        long reservationId = createPendingReservation(USER_1, 11);

        mockMvc.perform(post("/api/reservations/{reservationId}/fail-payment", reservationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "test-payment-failed-001"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", USER_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 11)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void cancelPendingReservation_releasesSeats() throws Exception {
        long reservationId = createPendingReservation(USER_1, 12);

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId)
                        .header("X-Debug-User", USER_1))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", USER_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private long createPendingReservation(String auth0Sub, long seatId) throws Exception {
        String responseBody = mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", auth0Sub)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, seatId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return extractReservationId(responseBody);
    }

    private void seedUser(String auth0Sub) {
        userRepository.findByAuth0Sub(auth0Sub).orElseGet(() -> {
            User user = new User();
            user.setAuth0Sub(auth0Sub);
            user.setEmail(auth0Sub + "@example.com");
            user.setFullName("Test " + auth0Sub);
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            return userRepository.save(user);
        });
    }

    private String seatsRequestBody(long showtimeId, long... seatIds) {
        StringBuilder body = new StringBuilder();
        body.append("{\"showtimeId\":").append(showtimeId).append(",\"seatIds\":[");

        for (int index = 0; index < seatIds.length; index++) {
            if (index > 0) {
                body.append(",");
            }

            body.append(seatIds[index]);
        }

        body.append("]}");
        return body.toString();
    }

    private long extractReservationId(String json) {
        String marker = "\"reservationId\":";
        int markerIndex = json.indexOf(marker);

        if (markerIndex < 0) {
            throw new IllegalStateException("reservationId not found: " + json);
        }

        int valueStart = markerIndex + marker.length();
        int valueEnd = json.indexOf(",", valueStart);

        if (valueEnd < 0) {
            valueEnd = json.indexOf("}", valueStart);
        }

        return Long.parseLong(json.substring(valueStart, valueEnd).trim());
    }
}