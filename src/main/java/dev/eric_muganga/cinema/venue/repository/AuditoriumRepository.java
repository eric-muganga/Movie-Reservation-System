package dev.eric_muganga.cinema.venue.repository;

import dev.eric_muganga.cinema.venue.entity.Auditorium;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    Optional<Auditorium> findByName(String name);
}
