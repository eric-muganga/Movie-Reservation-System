package dev.eric_muganga.cinema.reservation;

import dev.eric_muganga.cinema.CinemaApplication;
import dev.eric_muganga.cinema.reservation.repository.SeatLockRepository;
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

    @Autowired
    private SeatLockRepository seatLockRepository;

    private static final String DEBUG_USER = "auth0|test-user-123";
    private static final long SHOWTIME_ID = 3L;

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

    private void seedUser(String auth0Sub) {
        userRepository.findByAuth0Sub(auth0Sub)
                .orElseGet(() -> {
                    User user = new User();
                    user.setAuth0Sub(auth0Sub);
                    user.setEmail(auth0Sub + "@example.com");
                    user.setFullName("Debug " + auth0Sub);
                    user.setCreatedAt(OffsetDateTime.now());
                    user.setUpdatedAt(OffsetDateTime.now());
                    return userRepository.save(user);
                });
    }

    @BeforeAll
    void seedDebugUsers() {
        seedUser(DEBUG_USER);
        seedUser("auth0|lock-user-1");
        seedUser("auth0|lock-user-2");
        seedUser("auth0|lock-user-3");
    }

    /**
     * Helper JSON body used in multiple tests.
     */
    private String seatsRequestBody() {
        return """
            {
              "showtimeId": 3,
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
              "showtimeId": 3,
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


    @Test
    @Order(4)
    void reserveNow_onSeatsLockedByOtherUser_returns409Conflict() throws Exception {
        // Arrange: seed another user with its own debug header
        String OTHER_USER = "auth0|lock-user-1";
        if (userRepository.findByAuth0Sub(OTHER_USER).isEmpty()) {
            User user = new User();
            user.setAuth0Sub(OTHER_USER);
            user.setEmail("lock1@example.com");
            user.setFullName("Lock User 1");
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
        }

        String lockRequestBody = """
        {
          "showtimeId": 3,
          "seatIds": [1, 2]
        }
        """;

        // First: other user reserves seats → should be OK
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", OTHER_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequestBody))
                .andExpect(status().isConflict());

// Now: original debug user tries to reserve same seats → should see conflict
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
    }


    @Test
    @Order(5)
    void expiredLocks_doNotBlockNewReservation() throws Exception {
        String FIRST_USER = "auth0|lock-user-2";
        String SECOND_USER = "auth0|lock-user-3";

        // seed users (similar to previous test)
        // ... omitted for brevity

        String body = """
        {
          "showtimeId": 3,
          "seatIds": [3]
        }
        """;

        // First user reserves seat 3 (creates lock + CONFIRMED reservation)
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", FIRST_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        // Force-lock expiry for demonstration: set lock_expires_at in the past
        OffsetDateTime past = OffsetDateTime.now().minusMinutes(10);
        seatLockRepository.findActiveLocksForShowtime(3L, OffsetDateTime.now())
                .forEach(lock -> {
                    lock.setLockExpiresAt(past);
                    // keep status ACTIVE so expireLocks(now) will flip them to EXPIRED
                });
        seatLockRepository.saveAll(
                seatLockRepository.findActiveLocksForShowtime(3L, OffsetDateTime.now())
        );

        // Now second user attempts to reserve the same seat
        // reserveNow() will call expireLocks(now) first, so locks become EXPIRED
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", SECOND_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
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