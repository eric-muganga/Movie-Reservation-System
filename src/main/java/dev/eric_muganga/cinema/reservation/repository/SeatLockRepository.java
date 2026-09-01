package dev.eric_muganga.cinema.reservation.repository;

import dev.eric_muganga.cinema.reservation.entity.SeatLock;
import dev.eric_muganga.cinema.reservation.entity.SeatLockStatus;
import dev.eric_muganga.cinema.showtime.entity.Showtime;
import dev.eric_muganga.cinema.venue.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update SeatLock l
           set l.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.EXPIRED
         where l.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.ACTIVE
           and l.lockExpiresAt <= :now
    """)
    int expireLocks(@Param("now") OffsetDateTime now);

    Optional<SeatLock> findByShowtimeAndSeatAndStatus(
            Showtime showtime,
            Seat seat,
            SeatLockStatus status
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update SeatLock sl
           set sl.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.EXPIRED
         where sl.user.id = :userId
           and sl.showtime.id = :showtimeId
           and sl.seat.id in :seatIds
           and sl.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.ACTIVE
           and sl.lockExpiresAt > :now
    """)
    int releaseActiveLocks(
            @Param("userId") Long userId,
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") List<Long> seatIds,
            @Param("now") OffsetDateTime now
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update SeatLock sl
           set sl.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.EXPIRED
         where sl.user.id = :userId
           and sl.showtime.id = :showtimeId
           and sl.status = dev.eric_muganga.cinema.reservation.entity.SeatLockStatus.ACTIVE
           and sl.lockExpiresAt > :now
    """)
    int releaseActiveLocksForUserAndShowtime(
            @Param("userId") Long userId,
            @Param("showtimeId") Long showtimeId,
            @Param("now") OffsetDateTime now
    );

    List<SeatLock> findByShowtimeIdAndUserIdAndStatus(
            Long showtimeId,
            Long userId,
            SeatLockStatus status
    );
}
