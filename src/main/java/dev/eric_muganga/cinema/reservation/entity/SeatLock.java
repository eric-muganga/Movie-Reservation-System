package dev.eric_muganga.cinema.reservation.entity;

import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.user.entity.User;
import dev.eric_muganga.cinema.venue.entity.Seat;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "seat_locks",
        indexes = {
                @Index(
                        name = "idx_seat_locks_showtime_active",
                        columnList = "showtime_id, lock_expires_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id")
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @Column(name = "locked_at", nullable = false)
    private OffsetDateTime lockedAt;

    @Column(name = "lock_expires_at", nullable = false)
    private OffsetDateTime lockExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatLockStatus status = SeatLockStatus.ACTIVE;
}
