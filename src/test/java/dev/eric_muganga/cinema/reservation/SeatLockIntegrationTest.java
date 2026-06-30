package dev.eric_muganga.cinema.reservation;

import dev.eric_muganga.cinema.CinemaApplication;
import dev.eric_muganga.cinema.user.entity.User;
import dev.eric_muganga.cinema.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CinemaApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SeatLockIntegrationTest {

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

    private static final String USER_1 = "auth0|lock-user-1";
    private static final String USER_2 = "auth0|lock-user-2";
    private static final long SHOWTIME_ID = 3L;
    private static final long SEAT_ID = 1L;

    @BeforeAll
    void seedUsers() {
        seedDebugUser(USER_1);
        seedDebugUser(USER_2);
    }

    private void seedDebugUser(String auth0Sub) {
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

    private String lockRequestBody() {
        return """
            {
              "showtimeId": %d,
              "seatIds": [%d]
            }
            """.formatted(SHOWTIME_ID, SEAT_ID);
    }

    @Test
    void lockSeat_conflictingLockReturns409() throws Exception {
        // First user successfully locks the seat
        mockMvc.perform(post("/api/reservations/locks")
                        .header("X-Debug-User", USER_1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequestBody()))
                .andExpect(status().isCreated());

        // Second user tries to lock the same seat → 409 Conflict
        mockMvc.perform(post("/api/reservations/locks")
                        .header("X-Debug-User", USER_2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lockRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error", containsString("Conflict")))
                .andExpect(jsonPath("$.message", containsString("currently locked (LOCKED) by another user")));

    }
}