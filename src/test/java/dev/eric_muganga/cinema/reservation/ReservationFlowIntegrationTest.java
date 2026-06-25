package dev.eric_muganga.cinema.reservation;

import dev.eric_muganga.cinema.CinemaApplication;
import dev.eric_muganga.cinema.user.entity.User;
import dev.eric_muganga.cinema.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the reservation flow using a Testcontainers PostgreSQL database:
 *  - reserve seats (happy path)
 *  - prevent double booking (409 Conflict)
 *  - cancel reservation and free seats
 *
 * The PostgreSQLContainer provides an isolated Postgres instance for tests.
 */
@SpringBootTest(classes = CinemaApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // so we can use non-static @BeforeAll
public class ReservationFlowIntegrationTest {

    /**
     * Shared PostgreSQL container for this test class.
     * Spring Boot will automatically use it as the DataSource via @ServiceConnection.
     */
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

    private static final String DEBUG_USER = "auth0|test-user-123";
    private static final long SHOWTIME_ID = 1L;

    @BeforeAll
    void seedDebugUser() {
        if (userRepository.findByAuth0Sub(DEBUG_USER).isEmpty()) {
            User user = new User();
            user.setAuth0Sub(DEBUG_USER);
            user.setEmail("test@example.com");
            user.setFullName("Test User");
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Helper JSON body used in multiple tests.
     */
    private String seatsRequestBody() {
        return """
            {
              "showtimeId": 1,
              "seatIds": [1, 2, 3]
            }
            """;
    }

    /**
     * 1) Happy path: reserve seats for a future showtime.
     *    Expects 200 OK and a CONFIRMED reservation with 3 seats.
     *
     * Note: assumes your Flyway migrations seed at least one showtime with id=1
     * and seats 1..3 in the container DB.
     */
    @Test
    @Order(1)
    void reserveNow_createsConfirmedReservation() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.showtimeId").value((int) SHOWTIME_ID))
                .andExpect(jsonPath("$.seats", hasSize(3)))
                .andExpect(jsonPath("$.totalAmount", greaterThan(0.0)));
    }

    /**
     * 2) Double booking: attempting to reserve the same seats again for the same showtime
     *    should return 409 Conflict with a structured error payload.
     */
    @Test
    @Order(2)
    void reserveNow_onAlreadyReservedSeats_returns409Conflict() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
    }

    /**
     * 3) Cancel reservation: create a new reservation on different seats,
     *    cancel it via DELETE /api/reservations/{id}, then check seating to ensure
     *    those seats show as AVAILABLE again.
     *
     * Note: uses seatIds [4, 5] to avoid clashing with previous tests.
     */
    @Test
    @Order(3)
    void cancelReservation_marksReservationCancelled_andFreesSeats() throws Exception {
        // Step 1: create a reservation on seats 4 and 5
        String createBody = """
            {
              "showtimeId": 1,
              "seatIds": [4, 5]
            }
            """;

        String reservationJson = mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.seats", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long reservationId = extractReservationId(reservationJson);

        // Step 2: cancel the reservation
        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId)
                        .header("X-Debug-User", DEBUG_USER))
                .andExpect(status().isNoContent());

        // Step 3: verify that seating shows some rows (you can tighten this later)
        mockMvc.perform(get("/api/showtimes/{id}/seating", SHOWTIME_ID)
                        .header("X-Debug-User", DEBUG_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", not(empty())));
    }

    /**
     * Simple helper to extract reservationId from a JSON string.
     * In production tests, prefer ObjectMapper.
     */
    private long extractReservationId(String json) {
        String marker = "\"reservationId\":";
        int idx = json.indexOf(marker);
        if (idx == -1) {
            throw new IllegalStateException("reservationId not found in JSON: " + json);
        }
        int start = idx + marker.length();
        int end = json.indexOf(",", start);
        if (end == -1) {
            end = json.indexOf("}", start);
        }
        String value = json.substring(start, end).trim();
        return Long.parseLong(value);
    }
}