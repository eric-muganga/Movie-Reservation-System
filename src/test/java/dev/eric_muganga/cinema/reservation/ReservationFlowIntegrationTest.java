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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
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
    void seedUsers() {
        seedUser(DEBUG_USER);
        seedUser("auth0|lock-user-1");
        seedUser("auth0|lock-user-2");
        seedUser("auth0|lock-user-3");
    }

    private void seedUser(String auth0Sub) {
        userRepository.findByAuth0Sub(auth0Sub).orElseGet(() -> {
            User user = new User();
            user.setAuth0Sub(auth0Sub);
            user.setEmail(auth0Sub + "@example.com");
            user.setFullName("Debug " + auth0Sub);
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            return userRepository.save(user);
        });
    }

    private String seatsRequestBody(long showtimeId, long... seatIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"showtimeId\": ").append(showtimeId).append(",\n  \"seatIds\": [");
        for (int i = 0; i < seatIds.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(seatIds[i]);
        }
        sb.append("]\n}");
        return sb.toString();
    }

    @Test
    @Order(1)
    void reserveNow_createsConfirmedReservation() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 1, 2, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.showtimeId").value((int) SHOWTIME_ID))
                .andExpect(jsonPath("$.seats", hasSize(3)))
                .andExpect(jsonPath("$.totalAmount", greaterThan(0.0)));
    }

    @Test
    @Order(2)
    void reserveNow_onAlreadyReservedSeats_returns409Conflict() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 1, 2, 3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
    }

    @Test
    @Order(3)
    void cancelReservation_marksReservationCancelled_andFreesSeats() throws Exception {
        String reservationJson = mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 4, 5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.seats", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long reservationId = extractReservationId(reservationJson);

        mockMvc.perform(delete("/api/reservations/{reservationId}", reservationId)
                        .header("X-Debug-User", DEBUG_USER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/showtimes/{id}/seating", SHOWTIME_ID)
                        .header("X-Debug-User", DEBUG_USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", not(empty())));
    }

    @Test
    @Order(4)
    void reserveNow_onSeatsLockedByOtherUser_returns409Conflict() throws Exception {
        String otherUser = "auth0|lock-user-1";

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", otherUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 1, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message",
                        anyOf(containsString("already reserved"), containsString("locked"))));

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", DEBUG_USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seatsRequestBody(SHOWTIME_ID, 1, 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message",
                        anyOf(containsString("already reserved"), containsString("locked"))));
    }

    @Test
    @Order(5)
    void expiredLocks_doNotBlockNewReservation() throws Exception {
        String firstUser = "auth0|lock-user-2";
        String secondUser = "auth0|lock-user-3";
        String body = seatsRequestBody(SHOWTIME_ID, 6);

        String reservationJson = mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", firstUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long reservationId = extractReservationId(reservationJson);

        seatLockRepository.findActiveLocksForShowtime(SHOWTIME_ID, OffsetDateTime.now())
                .stream()
                .filter(lock -> lock.getSeat().getId().equals(6L))
                .forEach(lock -> {
                    lock.setLockExpiresAt(OffsetDateTime.now().minusMinutes(10));
                    lock.setStatus(SeatLockStatus.ACTIVE);
                });
        seatLockRepository.saveAll(seatLockRepository.findActiveLocksForShowtime(SHOWTIME_ID, OffsetDateTime.now()));

        mockMvc.perform(post("/api/reservations")
                        .header("X-Debug-User", secondUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("already reserved")));
    }

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
        return Long.parseLong(json.substring(start, end).trim());
    }
}