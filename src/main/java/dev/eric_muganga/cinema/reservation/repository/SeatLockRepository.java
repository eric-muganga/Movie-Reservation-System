package dev.eric_muganga.cinema.reservation.repository;

import dev.eric_muganga.cinema.reservation.entity.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {
    @Query("""
        select l
        from SeatLock l
        where l.showtime.id = :showtimeId
            and l.lockExpiresAt > :now
            and l.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.ACTIVE
    """)
    List<SeatLock> findActiveLocksForShowtime(
        @Param("showtimeId") long showtimeId,
        @Param("now") OffsetDateTime now
    );

    @Modifying
    @Query("""
        update SeatLock l
            set l.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.EXPIRED
        where l.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.ACTIVE
            and l.lockExpiresAt > :now
    """)
    int expireLocks(
            @Param("now") OffsetDateTime now
    );
}
