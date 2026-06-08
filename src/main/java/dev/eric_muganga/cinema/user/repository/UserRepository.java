package dev.eric_muganga.cinema.user.repository;

import dev.eric_muganga.cinema.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAuthOSub(String authOSub);
    Optional<User> findByEmail(String email);
    boolean existsByAuthOSub(String authOSub);
    boolean existsByEmail(String email);
}